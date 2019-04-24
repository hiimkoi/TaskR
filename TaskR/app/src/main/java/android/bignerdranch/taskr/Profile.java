package android.bignerdranch.taskr;

import android.bignerdranch.taskr.database.LevelAndExpBaseHelper;
import android.content.ContentValues;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.lang.Math.round;

public class Profile extends AppCompatActivity {

    // Vars for recycler view
    private ArrayList<UUID> mIds = new ArrayList<>();
    private ArrayList<String> mTaskTitles = new ArrayList<>();
    private ArrayList<String> mDatesNTimes = new ArrayList<>();
    private ArrayList<Task> listOfTasks = MainActivity.getTasks();

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    startActivity(new Intent(Profile.this, MainActivity.class));
                    return true;
                case R.id.navigation_dashboard:
                    startActivity(new Intent(Profile.this, CalendarActivity.class));
                    return true;
                case R.id.navigation_notifications:
                    //startActivity(new Intent(Profile.this, Profile.class));
                    return true;
            }
            return false;
        }
    };

    public static final double SCALE = 1.1; //Scale determines how fast to scale the xpToLevel.
    public static final int XP_BASE = 10;
    private static ProgressBar mProgressBar;
    private TextView mExperienceCounter;
    private static TextView mLevels;

    //creates User
    public List<User> listOfUsers = MainActivity.getUsers();

    private static int currentExp;
    private static int currentLevel;

    //this is for the test button
    private int i = 0;

    private static int xpToLevel = XP_BASE;

    Random random = new Random(); //for xp granting


    //private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_profile);
        initUser();
        xpToLevel =(int)Math.pow((XP_BASE * currentLevel), SCALE); //makes sure the xpToLevel is consistent every startup

        mProgressBar = findViewById(R.id.progressingBar);
        mExperienceCounter = findViewById(R.id.textViewExperience);
        mLevels = findViewById(R.id.textViewLevel);


        //Set text here just to display it properly between screens :^)
        mLevels.setText(currentLevel + "");
        mExperienceCounter.setText(currentExp + " / " + xpToLevel );
        mProgressBar.setProgress(currentExp);
        mProgressBar.setMax(xpToLevel);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        initTasks();

        //instantly does all the exp checking instead once per screen refresh
        if (MainActivity.firstStart) //Refer to MainActivity.java line:58
        {
            for(int j = 0; j < mIds.size(); j++)
            {
                expCheck();
            }
        }

        if (!MainActivity.firstStart)//Refer to MainActivity.java line:58
        {
            MainActivity.globalTaskFinishedCounter = mIds.size();
            MainActivity.firstStart = true;
        }

        mProgressBar.setProgress(currentExp);
        defineButtons();
    }


    //made a function to update the user for easier use.
    private static void updateUser(int level, int exp)
    {
        User newUser = new User(level, exp);
        MainActivity.updateUser(newUser);
    }

    /*
    TODO - done
    Note to self: Image view, in XML, just change to TextView
    take out other stuff
    make text view to just an incrementing number
     */

    //Note: Level can't go past 3 digits otherwise it starts looking ugly, but
    // I mean you can just make a check
    // however comma it's pointless
    public static void isLevelUp()
    {
        //THIS LOGIC FIXES THE OVERFLOW ISSUE. NEVER COMPARE PROGRESSBAR.PROGRESS
        if (currentExp >= xpToLevel)
        {
            mProgressBar.setProgress(0);
            currentLevel++;
            xpToLevel = (int)Math.pow((XP_BASE * currentLevel), SCALE); // Algorithm to determine how many exp for next level
            mProgressBar.setMax(xpToLevel);
            currentExp = 0;
            mLevels.setText(currentLevel + ""); //no idea why you need to add a blank thing but ok
        }
    }


    public void defineButtons() {
        findViewById(R.id.starButton).setOnClickListener(buttonClickListener);
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.starButton:
                    startActivity(new Intent(Profile.this, Rewards.class));
                    break;
            }
        }
    };

    /*
    //TODO: whenever task diffictuly gets implemented
    when a new task gets added to the "completed" array
    check the difficulty
    switch(difficulty)
        if quick: while loop 5 times
        if normal: while loop 10 times
        if long: while loop 20 times
    I think that should be it
     */

    private void initTasks() {

        //TODO: Should check to make sure it is working 100%


        for(int i = 0; i < listOfTasks.size(); i++)
        {
            if (listOfTasks.get(i).isCompleted()) {
                mIds.add(listOfTasks.get(i).getId());
                mTaskTitles.add(listOfTasks.get(i).getmName());
                mDatesNTimes.add(listOfTasks.get(i).getmDateAndTimeDue());
            }
        }

        initRecyclerView();
    }

    private void initUser()
    {
        //it specifically gets the zeroth position cause that's what it should always do
        //#SoloUserApplication btw
        User user = listOfUsers.get(0);
        currentLevel = user.getLevel();
        currentExp = user.getExpToNextLevel();
    }

    public void expCheck()
    {
        if (MainActivity.globalTaskFinishedCounter < mIds.size())
        {
            addExp();

            MainActivity.globalTaskFinishedCounter++;
        }
        //not tested but possible bug:
        //if you delete task, then immediately complete a new one
        //(i.e. profile > delete task > home > complete task > profile)
        //you might not get xp

        //as of two minutes later(11:37pm, 230419), can confirm.
        //i can imagine that it has to do with the fact that deleting a task
        //immediately brings you back to home rather than bringing you back to profile
        //that's the only fix i can imagine, because it would reset the counter!
        else if (MainActivity.globalTaskFinishedCounter > mIds.size())
        {
            while(MainActivity.globalTaskFinishedCounter > mIds.size())
            {
                MainActivity.globalTaskFinishedCounter--;
            }
        }
    }

    private void addExp()
    {
        //Generate random xp from 5-25
        int randomXP = random.nextInt(25-5) + 5;
        int xpLoop = 0; //just a counter for the while loop
        while(xpLoop < randomXP)// loops through giving xp
        {
            isLevelUp();
            mProgressBar.incrementProgressBy(1);
            currentExp +=1;
            mExperienceCounter.setText(currentExp + " / " + xpToLevel );
            xpLoop++;
        }
        updateUser(currentLevel,currentExp);
    }


    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.RecyclerViewProfile);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mIds, mTaskTitles, mDatesNTimes);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }



}