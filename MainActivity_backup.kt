package com.dinbanaye.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore by preferencesDataStore("dinbanaye")
private const val CHANNEL_ID = "routine_reminders"

data class RoutineTask(
    val id: Int, val title: String, val time: String, val section: String,
    val repeat: String = "Every day", val doneDates: Set<String> = emptySet(),
    val reminder: Boolean = false
)

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "Routine reminders", NotificationManager.IMPORTANCE_HIGH
        ))
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { DinBanayeApp() }
    }
}

fun dateKey(date: Date = Date()) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

fun currentStreak(tasks: List<RoutineTask>): Int {
    if (tasks.isEmpty()) return 0
    val cal = Calendar.getInstance()
    var count = 0
    while (true) {
        val d = dateKey(cal.time)
        val applicable = tasks.filter {
            when (it.repeat) {
                "Weekdays" -> cal.get(Calendar.DAY_OF_WEEK) !in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
                "Weekends" -> cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
                else -> true
            }
        }
        if (applicable.isNotEmpty() && applicable.all { d in it.doneDates }) {
            count++; cal.add(Calendar.DAY_OF_YEAR, -1)
        } else break
    }
    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinBanayeApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<RoutineTask>>(emptyList()) }
    var dark by remember { mutableStateOf(false) }
    var onboard by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf("Home") }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineTask?>(null) }
    val today = dateKey()

    LaunchedEffect(Unit) {
        val p = context.dataStore.data.first()
        dark = p[booleanPreferencesKey("dark")] ?: false
        onboard = !(p[booleanPreferencesKey("onboard")] ?: false)
        tasks = decodeTasks(p[stringPreferencesKey("tasks")] ?: "")
        if (tasks.isEmpty()) {
            tasks = listOf(
                RoutineTask(1,"Wake Up","6:30 AM","Morning"),
                RoutineTask(2,"Exercise","7:00 AM","Morning"),
                RoutineTask(3,"Breakfast","8:00 AM","Morning"),
                RoutineTask(4,"Work","9:30 AM","Afternoon"),
                RoutineTask(5,"Lunch","1:00 PM","Afternoon"),
                RoutineTask(6,"Walk","7:00 PM","Evening"),
                RoutineTask(7,"Dinner","8:30 PM","Night"),
                RoutineTask(8,"Sleep","10:30 PM","Night")
            )
        }
        ready = true
    }

    fun save(value: List<RoutineTask>) {
        tasks = value
        scope.launch { context.dataStore.edit { it[stringPreferencesKey("tasks")] = encodeTasks(value) } }
    }

    if (!ready) return

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(primary = Color(0xFF9C94FF))
        else lightColorScheme(primary = Color(0xFF6C63FF), background = Color(0xFFF7F5FF))
    ) {
        if (onboard) {
            Onboarding {
                onboard = false
                scope.launch { context.dataStore.edit { it[booleanPreferencesKey("onboard")] = true } }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("DinBanaye", fontWeight = FontWeight.Bold)
                                Text(
                                    when (screen) {
                                        "Home" -> SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
                                        "Calendar" -> "Your weekly routine"
                                        else -> "Your routine insights"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                dark = !dark
                                scope.launch { context.dataStore.edit { it[booleanPreferencesKey("dark")] = dark } }
                            }) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Theme") }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(selected=screen=="Home", onClick={screen="Home"}, icon={Icon(Icons.Default.Home, null)}, label={Text("Home")})
                        NavigationBarItem(selected=screen=="Calendar", onClick={screen="Calendar"}, icon={Icon(Icons.Default.DateRange, null)}, label={Text("Calendar")})
                        NavigationBarItem(selected=screen=="Stats", onClick={screen="Stats"}, icon={Icon(Icons.Default.BarChart, null)}, label={Text("Stats")})
                    }
                },
                floatingActionButton = {
                    if (screen == "Home") FloatingActionButton(onClick={showAdd=true}) {
                        Icon(Icons.Default.Add, "Add")
                    }
                }
            ) { pad ->
                when (screen) {
                    "Home" -> HomeScreen(
                        tasks, today, pad,
                        { task -> save(tasks.map { if (it.id == task.id) it.copy(
                            doneDates = if (today in it.doneDates) it.doneDates - today else it.doneDates + today
                        ) else it }) },
                        { editing = it },
                        { task -> save(tasks.filterNot { it.id == task.id }) }
                    )
                    "Calendar" -> CalendarScreen(tasks, pad)
                    else -> StatsScreen(tasks, pad)
                }
            }
            if (showAdd) TaskDialog(null, {showAdd=false}) {title,time,section,repeat,reminder ->
                val id=(tasks.maxOfOrNull{it.id} ?: 0)+1
                save(tasks + RoutineTask(id,title,time,section,repeat,emptySet(),reminder))
                showAdd=false
            }
            editing?.let { old ->
                TaskDialog(old,{editing=null}) {title,time,section,repeat,reminder ->
                    save(tasks.map { if(it.id==old.id) old.copy(title=title,time=time,section=section,repeat=repeat,reminder=reminder) else it })
                    editing=null
                }
            }
        }
    }
}

@Composable
fun Onboarding(done:()->Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF7F5FF)), contentAlignment=Alignment.Center) {
        Card(Modifier.padding(24.dp), shape=RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(30.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                Text("✓", fontSize=54.sp, color=Color(0xFF6C63FF), fontWeight=FontWeight.Bold)
                Text("DinBanaye", fontSize=34.sp, fontWeight=FontWeight.Bold, color=Color(0xFF6C63FF))
                Spacer(Modifier.height(8.dp))
                Text("Plan your day. Build your routine.", fontSize=18.sp)
                Spacer(Modifier.height(12.dp))
                Text("Tasks • Reminders • Streaks • Progress", color=Color.Gray)
                Spacer(Modifier.height(28.dp))
                Button(done, Modifier.fillMaxWidth()) { Text("Get Started") }
            }
        }
    }
}

@Composable
fun HomeScreen(
    tasks:List<RoutineTask>, today:String, pad:PaddingValues,
    onToggle:(RoutineTask)->Unit, onEdit:(RoutineTask)->Unit, onDelete:(RoutineTask)->Unit
) {
    val done=tasks.count{today in it.doneDates}; val p=if(tasks.isEmpty())0f else done.toFloat()/tasks.size
    LazyColumn(
        Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Card(shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                        Column { Text("Today's Progress", fontSize=18.sp, fontWeight=FontWeight.Bold); Text("$done of ${tasks.size} tasks", fontSize=13.sp, color=Color.Gray) }
                        Text("${(p*100).toInt()}%", fontSize = 26.sp, fontWeight=FontWeight.Bold, color=Color(0xFF6C63FF))
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator({p}, Modifier.fillMaxWidth().height(9.dp))
                }
            }
        }
        item { Card(shape=RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                Text("🔥",fontSize=26.sp);Spacer(Modifier.width(10.dp));Column{Text("${currentStreak(tasks)} day streak",fontWeight=FontWeight.Bold);Text("Keep going!", fontSize=13.sp, color=Color.Gray)}
            }
        }}
        listOf("Morning","Afternoon","Evening","Night").forEach { sec ->
            val group=tasks.filter{it.section==sec}
            if(group.isNotEmpty()){
                item { Text(sec, fontSize=19.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=8.dp)) }
                items(group,key={it.id}){task->TaskCard(task,today,onToggle,onEdit,onDelete)}
            }
        }
        item{Spacer(Modifier.height(80.dp))}
    }
}

@Composable
fun TaskCard(task:RoutineTask,today:String,onToggle:(RoutineTask)->Unit,onEdit:(RoutineTask)->Unit,onDelete:(RoutineTask)->Unit){
    Card(shape=RoundedCornerShape(17.dp)){
        Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.CenterVertically){
            Checkbox(today in task.doneDates,{onToggle(task)})
            Column(Modifier.weight(1f)){Text(task.title,fontWeight=FontWeight.SemiBold);Text("${task.time} • ${task.repeat}", fontSize=12.sp, color=Color.Gray)}
            IconButton({onEdit(task)}){Icon(Icons.Default.Edit,"Edit")}
            IconButton({onDelete(task)}){Icon(Icons.Default.Delete,"Delete",tint=Color.Gray)}
        }
    }
}

@Composable
fun CalendarScreen(tasks:List<RoutineTask>,pad:PaddingValues){
    val cal=Calendar.getInstance();cal.set(Calendar.DAY_OF_WEEK,cal.firstDayOfWeek)
    Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)){
        Text("This Week", fontSize=24.sp, fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp))
        repeat(7){i->val c=cal.clone() as Calendar;c.add(Calendar.DAY_OF_YEAR,i);val d=dateKey(c.time);val done=tasks.count{d in it.doneDates};val pct=if(tasks.isEmpty())0f else done.toFloat()/tasks.size
            Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(15.dp)){Column(Modifier.padding(14.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(SimpleDateFormat("EEE, dd MMM",Locale.getDefault()).format(c.time),fontWeight=FontWeight.SemiBold);Text("$done/${tasks.size}")}
                Spacer(Modifier.height(7.dp));LinearProgressIndicator({pct},Modifier.fillMaxWidth())
            }}
        }
    }
}

@Composable
fun StatsScreen(tasks:List<RoutineTask>,pad:PaddingValues){
    Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)){
        Text("Your Insights", fontSize=24.sp, fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp))
        val today=dateKey();val done=tasks.count{today in it.doneDates};val pct=if(tasks.isEmpty())0 else done*100/tasks.size
        Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(20.dp)){Text("Today", fontSize=18.sp, fontWeight=FontWeight.Bold);Text("$pct% complete", fontSize=30.sp, fontWeight=FontWeight.Bold, color=Color(0xFF6C63FF));Text("$done / ${tasks.size} tasks")}}
        Spacer(Modifier.height(12.dp))
        Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(20.dp)){Text("🔥 Current Streak", fontSize=18.sp, fontWeight=FontWeight.Bold);Text("${currentStreak(tasks)} days", fontSize=30.sp, fontWeight=FontWeight.Bold);Text("Complete your routine daily to grow it.")}}
    }
}

@Composable
fun TaskDialog(existing:RoutineTask?,dismiss:()->Unit,onSave:(String,String,String,String,Boolean)->Unit){
    var title by remember{mutableStateOf(existing?.title?:"")};var time by remember{mutableStateOf(existing?.time?:"")}
    var section by remember{mutableStateOf(existing?.section?:"Morning")};var repeat by remember{mutableStateOf(existing?.repeat?:"Every day")}
    var reminder by remember{mutableStateOf(existing?.reminder?:false)}
    AlertDialog(onDismissRequest=dismiss,title={Text(if(existing==null)"Add Routine" else "Edit Routine")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(title,{title=it},label={Text("Task name")},singleLine=true)
            OutlinedTextField(time,{time=it},label={Text("Time")},singleLine=true)
            Text("Section");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){listOf("Morning","Afternoon","Evening","Night").forEach{x->FilterChip(section==x,{section=x},{Text(x)})}}
            Text("Repeat");Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){listOf("Every day","Weekdays","Weekends").forEach{x->FilterChip(repeat==x,{repeat=x},{Text(x)})}}
            Row(verticalAlignment=Alignment.CenterVertically){Switch(reminder,{reminder=it});Spacer(Modifier.width(8.dp));Text("Reminder")}
        }
    },confirmButton={TextButton(enabled=title.isNotBlank(),onClick={onSave(title,time.ifBlank{"Any time"},section,repeat,reminder)}){Text("Save")}},
    dismissButton={TextButton(dismiss){Text("Cancel")}})
}

fun encodeTasks(tasks:List<RoutineTask>):String{
    val a=JSONArray();tasks.forEach{a.put(JSONObject().apply{
        put("id",it.id);put("title",it.title);put("time",it.time);put("section",it.section);put("repeat",it.repeat)
        put("done",JSONArray(it.doneDates.toList()));put("reminder",it.reminder)
    })};return a.toString()
}
fun decodeTasks(s:String):List<RoutineTask>{
    if(s.isBlank())return emptyList()
    return try{val a=JSONArray(s);List(a.length()){i->val o=a.getJSONObject(i);val set=mutableSetOf<String>();o.optJSONArray("done")?.let{j->for(k in 0 until j.length())set.add(j.getString(k))}
        RoutineTask(o.getInt("id"),o.getString("title"),o.getString("time"),o.getString("section"),o.optString("repeat","Every day"),set,o.optBoolean("reminder"))
    }}catch(_:Exception){emptyList()}
}
