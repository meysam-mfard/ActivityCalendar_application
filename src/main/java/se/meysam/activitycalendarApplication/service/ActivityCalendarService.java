package se.meysam.activitycalendarApplication.service;

import com.google.api.services.calendar.model.Event;
import se.lnu.advsd.meysam.ActivityCalendarApi.model.Activity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ActivityCalendarService {

    //Returns list of all activities
    Map<String, Activity> getActivities(String userEmail);


    //Returns percentage of time spent on each activity considering all activities in the specified calendar.
    Map<String, Double> getActivitiesSpentPercentage(String userEmail);

    //Returns "activity"
    Optional<Activity> getActivity(String userEmail, String activityName);

    void addActivity(String userEmail, Activity activity);

    Event addEvent(String userEmail, Event event, String activityName);

    Map<String, Event> get_Activity_to_Event_Map(String userEmail);

    List<Integer> getTest(Integer num);
}
