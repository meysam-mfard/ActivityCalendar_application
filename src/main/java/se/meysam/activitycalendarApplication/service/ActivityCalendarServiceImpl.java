package se.meysam.activitycalendarApplication.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.stereotype.Service;
import se.lnu.advsd.meysam.ActivityCalendarApi.ActivityCalendarApi;
import se.lnu.advsd.meysam.ActivityCalendarApi.ActivityCalendarApiImpl;
import se.lnu.advsd.meysam.ActivityCalendarApi.model.Activity;
import se.meysam.activitycalendarApplication.authorization.Authorize;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class ActivityCalendarServiceImpl implements ActivityCalendarService {

    private final Authorize authorization;
    private static final ActivityCalendarApi activityCalendarApi = new ActivityCalendarApiImpl();

    public ActivityCalendarServiceImpl(Authorize authorization) {
        this.authorization = authorization;
    }


    private String getCalendarId(String userEmail) {
        String calendarId = null;
        try {
            calendarId = authorization.getCalendar(userEmail).calendars().get(userEmail).execute().getId();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return calendarId;
    }

    @Override
    public Map<String, Activity> getActivities(String userEmail){
        return activityCalendarApi.getActivities(getCalendarId(userEmail));
    }

    @Override
    public Map<String, Double> getActivitiesSpentPercentage(String userEmail) {
        return activityCalendarApi.getActivitiesSpentPercentage(getCalendarId(userEmail));
    }

    @Override
    public Optional<Activity> getActivity(String userEmail, String activityName){
        return activityCalendarApi.getActivity(getCalendarId(userEmail), activityName);
    }

    @Override
    public void addActivity(String userEmail, Activity activity) {
        activityCalendarApi.addActivity(getCalendarId(userEmail), activity);
    }

    @Override
    public Event addEvent(String userEmail, Event event, String activityName) {
        try {
            return activityCalendarApi.addEvent(authorization.getCalendar(userEmail),getCalendarId(userEmail), activityName, event);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, Event> get_Activity_to_Event_Map(String userEmail) {
        Map<String, Activity> activities = getActivities(userEmail);
        if (activities == null)
            return new HashMap<>();
        Map<String, Event> activity_to_Event_Map = new HashMap<>(activities.size());
        Calendar calendar = null;
        try {
            calendar = authorization.getCalendar(userEmail);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        Events events = null;
        try {
            events = calendar.events().list(userEmail).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Event> eventList = events.getItems();
        for (Event event: eventList) {
            for (Activity activity:activities.values()) {
                if (activity.getEventsIds().contains(event.getId())) {
                    activity_to_Event_Map.put(activity.getName(), event);
                    break;
                }
            }
        }

        /*Event event;
        for (Activity activity:activities.values()) {
            for (String eventId: activity.getEventsIds()) {
                try {
                    event = calendar.events().get(userEmail, eventId).execute();
                    activity_to_Event_Map.put(activity.getName(), event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/

        return activity_to_Event_Map;
    }

    @Override
    public List<Integer> getTest(Integer num) {
        return Arrays.asList(num, num, num);
    }
}
