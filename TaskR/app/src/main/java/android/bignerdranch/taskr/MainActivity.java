package android.bignerdranch.taskr;

import android.app.Activity;
import android.content.res.TypedArray;
import android.support.v7.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.bignerdranch.taskr.database.LevelAndExpBaseHelper;
import android.bignerdranch.taskr.database.LevelAndExpCursorWrapper;
import android.bignerdranch.taskr.database.LevelAndExpDbSchema;
import android.bignerdranch.taskr.database.TaskBaseHelper;
import android.bignerdranch.taskr.database.TaskCursorWrapper;
import android.bignerdranch.taskr.database.TaskDbSchema;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static Context mContext;
    private static SQLiteDatabase mDatabase;
    private static SQLiteDatabase mLevelAndExpDatabase;
    private static TaskBaseHelper mTaskBaseInstance = null;
    private static LevelAndExpBaseHelper mUserBaseInstance = null;

    // Vars for RecyclerView
    private ArrayList<UUID> mIds = new ArrayList<>();
    private ArrayList<String> mTaskTitles = new ArrayList<>();
    private ArrayList<String> mDatesNTimes = new ArrayList<>();
    private ArrayList<String> mDatesCreated = new ArrayList<>();
    private ArrayList<String> mDifficulties = new ArrayList<>();
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private TimePickerDialog.OnTimeSetListener mTimeSetListener;

    // all necessary for exp stuff
    public static int globalTaskFinishedCounter = 0; //counter for the tasks
    private static boolean turnOnUser = false; //honestly forgot why i needed it, but it's necessary
    public static boolean firstStart = false; //needed to ensure extra xp isn't granted on startup
//    public static int mIdsSizeForRewards = 0;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    finish();
                    startActivity(new Intent(MainActivity.this, CalendarActivity.class));
                    return true;
                case R.id.navigation_notifications:
                    finish();
                    startActivity(new Intent(MainActivity.this, Profile.class));
                    return true;
                case R.id.navigation_rewards:
                    finish();
                    startActivity(new Intent(MainActivity.this, Rewards.class));
                    return true;
            }
            return false;
        }
    };

    public static TaskBaseHelper getInstanceTaskBase(Context context)
    {
        if (mTaskBaseInstance == null)
            mTaskBaseInstance = new TaskBaseHelper(mContext);

        return mTaskBaseInstance;
    }

    public static LevelAndExpBaseHelper getInstanceUserBase(Context context)
    {
        if (mUserBaseInstance == null)
            mUserBaseInstance = new LevelAndExpBaseHelper(mContext);

        return mUserBaseInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
//        mDatabase = new TaskBaseHelper(mContext).getWritableDatabase();
//        mLevelAndExpDatabase = new LevelAndExpBaseHelper(mContext).getWritableDatabase();

        mDatabase = getInstanceTaskBase(mContext).getWritableDatabase();
        mLevelAndExpDatabase = getInstanceUserBase(mContext).getWritableDatabase();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Spinner spinnerSort = findViewById(R.id.filterHomeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sort, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);
        //spinner.setOnItemSelectedListener(this); //TODO: Needs to be finished for sorting

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortOption = parent.getItemAtPosition(position).toString();

                if (sortOption.equals("Date Created - Newest to Oldest"))
                    sortTasksByDateCreatedNewestToOldest();
                else if (sortOption.equals("Date Created - Oldest to Newest"))
                    sortTasksByDateCreatedOldestToNewest();
                else if (sortOption.equals("Date Due"))
                    sortTasksByDateDue();
                else if (sortOption.equals("Difficulty - Quick, Normal, Long"))
                    sortTasksByDifficultyQuickNormalLong();
                else if (sortOption.equals("Difficulty - Long, Normal, Quick"))
                    sortTasksByDifficultyLongNormalQuick();
            }
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        defineButtons();

        initTasks();
        if (!turnOnUser)
        {
            initUsers();
            turnOnUser = true;
        }

    }

//    protected View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
//    {
//        //View view = inflater.inflate(R.layout.)
//
//    }

    private void initUsers()
    {
        List<User> listOfUsers = getUsers();

        for(int i = 0; i < listOfUsers.size(); i++)
        {
            if(listOfUsers.get(i).getName().equals("user"))
            {
                break;
            }
        }

        User user = new User(1, 0);
        MainActivity.addUser(user);
//        Task newTask = new Task(inputName.getText().toString(), inputDescription.getText().toString(),
//                inputDate.getText().toString() + " at " +
//                        inputTime.getText().toString());
//        MainActivity.addTask(newTask);

    }

    private void initTasks() {

        ArrayList<Task> listOfTasks = getTasks();

        for(int i = 0; i < listOfTasks.size(); i++)
        {
            if (!listOfTasks.get(i).isCompleted()) {
                mIds.add(listOfTasks.get(i).getId());
                mTaskTitles.add(listOfTasks.get(i).getmName());
                mDatesNTimes.add(listOfTasks.get(i).getmDateAndTimeDue());
                mDatesCreated.add(listOfTasks.get(i).getDateCreated());
                mDifficulties.add(listOfTasks.get(i).getDifficulty());
            }
        }

        initRecyclerView();
    }

    //initializes RecyclerView for the home screen
    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.RecyclerViewHome);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mIds, mTaskTitles, mDatesNTimes);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    //defines new task button
    public void defineButtons() {
        findViewById(R.id.NewTask_floatingActionButton).setOnClickListener(buttonClickListener);
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.NewTask_floatingActionButton:

                    final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                    View mView = inflater.inflate(R.layout.dialog_create, null);

                    final EditText inputName = (EditText) mView.findViewById(R.id.newTitle);
                    final TextView inputDate = (TextView) mView.findViewById(R.id.newDate);
                    final TextView inputTime = (TextView) mView.findViewById(R.id.newTime);
                    final EditText inputDescription = (EditText) mView.findViewById(R.id.newDescription);

                    Button mCancel = (Button) mView.findViewById(R.id.cancelButton);
                    Button mSave = (Button) mView.findViewById(R.id.saveButton);

                    final Spinner spinnerDifficulty = mView.findViewById(R.id.spinnerDifficult);
                    ArrayAdapter<CharSequence> adapterDifficulty = ArrayAdapter.createFromResource(mContext, R.array.difficulty, android.R.layout.simple_spinner_item);
                    adapterDifficulty.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDifficulty.setAdapter(adapterDifficulty);
                    //spinner.setOnItemSelectedListener(this); //TODO: Needs to be finished for difficulty

                    mBuilder.setView(mView);
                    final AlertDialog dialog = mBuilder.create();
                    dialog.show();

                    inputDate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Calendar cal = Calendar.getInstance();
                            int year = cal.get(Calendar.YEAR);
                            int month = cal.get(Calendar.MONTH);
                            int day = cal.get(Calendar.DAY_OF_MONTH);

                            DatePickerDialog dateDialog = new DatePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mDateSetListener, year, month, day);
                            dateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
                            dateDialog.show();

                        }
                    });

                    mDateSetListener = new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            month = month + 1;

                            String monthString;

                            String yearString = Integer.toString(year);

                            if (month < 10)     //useful for sorting purposes later on
                                monthString = "0" + Integer.toString(month);
                            else
                                monthString = Integer.toString(month);

                            String dayOfMonthString = Integer.toString(dayOfMonth);


                            String date = monthString + "/" + dayOfMonthString + "/" + yearString;
                            inputDate.setText(date);

                        }
                    };

                    inputTime.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Calendar cal = Calendar.getInstance();
                            int hour = cal.get(Calendar.HOUR);
                            int minutes = cal.get(Calendar.MINUTE);
                            boolean isTwentyFour = false;

                            TimePickerDialog timeDialog = new TimePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, mTimeSetListener, hour, minutes, isTwentyFour);
                            timeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
                            timeDialog.show();

                        }
                    });

                    mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                            String time = hourOfDay + ":" + minute;
                            //inputTime.setText(time);
                            int hour = hourOfDay % 12;
                            if (hour == 0)
                                hour = 12;
                            inputTime.setText(String.format("%02d:%02d %s", hour, minute,
                                    hourOfDay < 12 ? "AM" : "PM"));
                        }
                    };

                    mSave.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View view) {
                            //adding that new task object to the database itself

                            if (inputName.getText().toString().equals("") ||
                                inputDate.getText().toString().equals("") ||
                                inputTime.getText().toString().equals("") ||
                                inputDescription.getText().toString().equals("") ||
                                spinnerDifficulty.getSelectedItem().toString().equals("Difficulty"))
                            {
                                Toast.makeText(MainActivity.this, "Please fill in all fields!",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Task newTask = new Task(inputName.getText().toString(), inputDescription.getText().toString(),
                                        inputDate.getText().toString() + " at " +
                                                inputTime.getText().toString(), spinnerDifficulty.getSelectedItem().toString());

                                MainActivity.addTask(newTask);

                                dialog.dismiss();
                                recreate();
                            }
                        }
                    });

                    mCancel.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });

                    break;
            }
        }
    };

    //practically makes a new task
    private static ContentValues getContentValues(Task task)    {   //adds new task (should be in CreatingTask.java)
        ContentValues values = new ContentValues();
        values.put(TaskDbSchema.TaskTable.Cols.UUID, task.getId().toString());
        values.put(TaskDbSchema.TaskTable.Cols.NAME, task.getmName());
        values.put(TaskDbSchema.TaskTable.Cols.DESCRIPTION, task.getmDescription());
        values.put(TaskDbSchema.TaskTable.Cols.DATE_AND_TIME_DUE, task.getmDateAndTimeDue());
        values.put(TaskDbSchema.TaskTable.Cols.COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(TaskDbSchema.TaskTable.Cols.DIFFICULTY, task.getDifficulty());
        values.put(TaskDbSchema.TaskTable.Cols.DATE_CREATED, task.getDateCreated());

        return values;
    }

    private void sortTasksByDateCreatedOldestToNewest()
    {
        Collections.sort(mDatesCreated, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return object1.compareTo(object2);
            }
        });

        dateCreatedReinitializeRecyclerView();
    }

    private void sortTasksByDateCreatedNewestToOldest()
    {
        Collections.sort(mDatesCreated, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return object2.compareTo(object1);
            }
        });

        dateCreatedReinitializeRecyclerView();
    }

    private void dateCreatedReinitializeRecyclerView()
    {
        ArrayList<UUID> newIdList = new ArrayList<>();
        ArrayList<String> newTaskTitleList = new ArrayList<>();
        ArrayList<String> newDatesNTimesList = new ArrayList<>();
        ArrayList<String> newDifficultiesList = new ArrayList<>();

        //to prevent double adding if multiple tasks were created on the same day
        ArrayList<Boolean> addedArray = new ArrayList<>();

        for (int i = 0; i < mIds.size(); i++)
            addedArray.add(false);

        for (int i = 0; i < mDatesCreated.size(); i++)
        {
            for (int j = 0; j < mIds.size(); j++)
            {
                if (!addedArray.get(j) && getTask(mIds.get(j)).getDateCreated().equals(mDatesCreated.get(i)))
                {
                    newIdList.add(mIds.get(j));
                    newTaskTitleList.add(mTaskTitles.get(j));
                    newDatesNTimesList.add(mDatesNTimes.get(j));
                    newDifficultiesList.add(mDifficulties.get(j));
                    addedArray.set(j, true);
                }
            }
        }

        mIds = newIdList;
        mTaskTitles = newTaskTitleList;
        mDatesNTimes = newDatesNTimesList;
        mDifficulties = newDifficultiesList;

        initRecyclerView();
    }

    private void sortTasksByDateDue()
    {
        Collections.sort(mDatesNTimes, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return object1.compareTo(object2);
            }
        });

        dateDueReinitializeRecyclerView();
    }

    private void dateDueReinitializeRecyclerView()  //reinitializes the recycler view if the tasks are resorted by dates due
    {
        //align mIds, mTaskTitles, and mDatesCreated with mDateNTimes

        ArrayList<UUID> newIdList = new ArrayList<>();
        ArrayList<String> newTaskTitleList = new ArrayList<>();
        ArrayList<String> newDateCreatedList = new ArrayList<>();
        ArrayList<String> newDifficultiesList = new ArrayList<>();

        //to prevent double adding if multiple tasks were created on the same day
        ArrayList<Boolean> addedArray = new ArrayList<>();

        for (int i = 0; i < mIds.size(); i++)
            addedArray.add(false);

        for(int i = 0; i < mDatesNTimes.size(); i++)
        {
            for (int j = 0; j < mIds.size(); j++)
            {
                if (!addedArray.get(j) && getTask(mIds.get(j)).getmDateAndTimeDue().equals(mDatesNTimes.get(i)))
                {
                    newIdList.add(mIds.get(j));
                    newTaskTitleList.add(mTaskTitles.get(j));
                    newDateCreatedList.add(mDatesCreated.get(j));
                    newDifficultiesList.add(mDifficulties.get(j));
                    addedArray.set(j, true);
                }
            }
        }

        mIds = newIdList;
        mTaskTitles = newTaskTitleList;
        mDatesCreated = newDateCreatedList;
        mDifficulties = newDifficultiesList;

        initRecyclerView();
    }

    private void sortTasksByDifficultyQuickNormalLong()
    {
        Collections.sort(mDifficulties, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return object2.compareTo(object1);
            }
        });

        difficultyReinitializeRecyclerView();
    }

    private void sortTasksByDifficultyLongNormalQuick()
    {
        Collections.sort(mDifficulties, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return object1.compareTo(object2);
            }
        });

        difficultyReinitializeRecyclerView();
    }

    private void difficultyReinitializeRecyclerView()
    {
        //align mIds, mTaskTitles, and mDatesCreated with mDateNTimes

        ArrayList<UUID> newIdList = new ArrayList<>();
        ArrayList<String> newTaskTitleList = new ArrayList<>();
        ArrayList<String> newDateCreatedList = new ArrayList<>();
        ArrayList<String> newDatesNTimesList = new ArrayList<>();

        //to prevent double adding if multiple tasks were created on the same day
        ArrayList<Boolean> addedArray = new ArrayList<>();

        for (int i = 0; i < mIds.size(); i++)
            addedArray.add(false);

        for(int i = 0; i < mDatesNTimes.size(); i++)
        {
            for (int j = 0; j < mIds.size(); j++)
            {
                if (!addedArray.get(j) && getTask(mIds.get(j)).getDifficulty().equals(mDifficulties.get(i)))
                {
                    newIdList.add(mIds.get(j));
                    newTaskTitleList.add(mTaskTitles.get(j));
                    newDateCreatedList.add(mDatesCreated.get(j));
                    newDatesNTimesList.add(mDatesNTimes.get(j));
                    addedArray.set(j, true);
                }
            }
        }

        mIds = newIdList;
        mTaskTitles = newTaskTitleList;
        mDatesCreated = newDateCreatedList;
        mDatesNTimes = newDatesNTimesList;

        initRecyclerView();
    }

    public static void addTask(Task c) {
        ContentValues values = getContentValues(c);

        mDatabase.insert(TaskDbSchema.TaskTable.NAME, null, values);
    }

    public static Task getTask(UUID id)    {       //get specific task by uuid
        TaskCursorWrapper cursor = queryTasks(TaskDbSchema.TaskTable.Cols.UUID +
                " = ?", new String[] {id.toString()});

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            return cursor.getTask();
        } finally {
            cursor.close();
        }
    }

    public static void updateTask(UUID id, Task task)   {   //edits task accordingly

        //new task that will replace old task
        ContentValues values = getContentValues(task);

        mDatabase.update(TaskDbSchema.TaskTable.NAME, values, TaskDbSchema.TaskTable.Cols.UUID
                            + " = ?", new String[] { id.toString() });
    }


    public static void deleteTask(UUID taskID)    {
        mDatabase.delete(TaskDbSchema.TaskTable.NAME, TaskDbSchema.TaskTable.Cols.UUID
                            + " = ?", new String[] {taskID.toString()});

        //deletes tasks by searching by name (should change in the future, could accidentally delete a different task)



        //for test lol

    }

    private static TaskCursorWrapper queryTasks(String whereClause, String[] whereArgs)   {   //reading from database using query
        Cursor cursor = mDatabase.query(
                TaskDbSchema.TaskTable.NAME,
                null,    //columns - null selects all columns
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new TaskCursorWrapper(cursor);
    }

    public static ArrayList<Task> getTasks()    {       //get all tasks in the database and put them in an ArrayList
        ArrayList<Task> tasks = new ArrayList<>();

        TaskCursorWrapper cursor = queryTasks(null,null);

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast())   {
                tasks.add(cursor.getTask());
                cursor.moveToNext();
            }
        }   finally {
            cursor.close();
        }
        return tasks;
    }

    public static ContentValues getLevelAndExpContentValues(User user) {
        ContentValues values = new ContentValues();
        values.put(LevelAndExpDbSchema.LevelAndExpTable.Cols.NAME, user.getName());
        values.put(LevelAndExpDbSchema.LevelAndExpTable.Cols.LEVEL, user.getLevel());
        values.put(LevelAndExpDbSchema.LevelAndExpTable.Cols.EXP, user.getExpToNextLevel());

        return values;
    }

    public static void addUser(User user) {
        ContentValues values = getLevelAndExpContentValues(user);

        mLevelAndExpDatabase.insert(LevelAndExpDbSchema.LevelAndExpTable.NAME, null, values);
    }

//    public static void updateTask(UUID id, Task task)   {   //edits task accordingly
//
//        //new task that will replace old task
//        ContentValues values = getContentValues(task);
//
//        mDatabase.update(TaskDbSchema.TaskTable.NAME, values, TaskDbSchema.TaskTable.Cols.UUID
//                + " = ?", new String[] { id.toString() });
//    }

    public static void updateUser(User user) {
        String nameString = user.getName();
        ContentValues values = getLevelAndExpContentValues(user);

        mLevelAndExpDatabase.update(LevelAndExpDbSchema.LevelAndExpTable.NAME, values,
                LevelAndExpDbSchema.LevelAndExpTable.Cols.NAME + " = ?",
                new String[] {nameString});
    }

    private static LevelAndExpCursorWrapper queryLevelAndExp(String whereClause, String[] whereArgs) {
        Cursor cursor = mLevelAndExpDatabase.query(
                LevelAndExpDbSchema.LevelAndExpTable.NAME,
                null,   //columns - null selects all columns
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new LevelAndExpCursorWrapper(cursor);
    }

    public static List<User> getUsers() {
        List<User> users = new ArrayList<>();

//        TaskCursorWrapper cursor = queryTasks(TaskDbSchema.TaskTable.Cols.UUID +
//                " = ?", new String[] {id.toString()});
        LevelAndExpCursorWrapper cursor = queryLevelAndExp(null, null);

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                users.add(cursor.getExpAndLevel());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

        return users;
    }

    public static User getUser(String name) {
        LevelAndExpCursorWrapper cursor = queryLevelAndExp(
                LevelAndExpDbSchema.LevelAndExpTable.Cols.NAME + " = ?",
                new String[] { name });

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            return cursor.getExpAndLevel();
        } finally {
            cursor.close();
        }
    }
}
