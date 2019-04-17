package se.meysam.activitycalendarApplication.controller;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import se.lnu.advsd.meysam.ActivityCalendarApi.model.Activity;
import se.meysam.activitycalendarApplication.dto.ActivityDTO;
import se.meysam.activitycalendarApplication.dto.EventDTO;
import se.meysam.activitycalendarApplication.service.ActivityCalendarService;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class calendarController {

    private final ActivityCalendarService activityCalendarService;
    //Authentication variables:
    private static String authorizationRequestBaseUri = "oauth2/authorization";
    private ClientRegistrationRepository clientRegistrationRepository;//autowired
    private OAuth2AuthorizedClientService authorizedClientService;//autowired
    private String clientName;
    private String clientEmail;

    public calendarController(ActivityCalendarService activityCalendarService,
                              ClientRegistrationRepository clientRegistrationRepository,
                              OAuth2AuthorizedClientService authorizedClientService) {
        this.activityCalendarService = activityCalendarService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientService = authorizedClientService;
    }



    @GetMapping("/oauthLogin")
    public String getLoginPage(Model model) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");
        String oauth2AuthenticationUrl = authorizationRequestBaseUri + "/" + registration.getRegistrationId();
        model.addAttribute("url", oauth2AuthenticationUrl);

        return "oauthLogin";
    }


    @RequestMapping({"/", ""})
    public String displayCalendarPage(Model model, OAuth2AuthenticationToken authentication){

        //Getting users info
        OAuth2AuthorizedClient client = authorizedClientService
                .loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());
        String userInfoEndpointUri = client.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUri();
        if (!StringUtils.isEmpty(userInfoEndpointUri)) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken()
                    .getTokenValue());
            HttpEntity entity = new HttpEntity("", headers);
            ResponseEntity<Map> response = restTemplate
                    .exchange(userInfoEndpointUri, HttpMethod.GET, entity, Map.class);
            Map userAttributes = response.getBody();
            model.addAttribute("name", userAttributes.get("given_name"));
            model.addAttribute("email", userAttributes.get("email"));
            clientName = (String) userAttributes.get("given_name");
            clientEmail = (String) userAttributes.get("email");
        }


        //for displaying events in FullCalendar library
        model.addAttribute("events", getEventsList());


        //for displaying activities in Chart.js library
        Map<String, Activity> activityMap = activityCalendarService.getActivities(clientEmail);

        Map<String, Double> percentageMap = activityCalendarService.getActivitiesSpentPercentage(clientEmail);
        List<String> activitySpentPercentage = new ArrayList<>();

        List<String> activityNames = new ArrayList<>(activityMap.keySet());

        DecimalFormat df = new DecimalFormat("#.00");
        List<Integer> activitySpentBudgets = new ArrayList<>();
        List<Integer> activityBudgets = new ArrayList<>();
        List<Integer> activityRemainingBudgets = new ArrayList<>();
        activityMap.values().forEach(activity -> {
            activitySpentBudgets.add(activity.getSpentBudget());
            activityBudgets.add(activity.getBudget());
            activityRemainingBudgets.add(activity.getBudget() - activity.getSpentBudget());
            activitySpentPercentage.add(df.format(percentageMap.get(activity.getName())));
        });

        model.addAttribute("activityNames", activityNames);
        model.addAttribute("spentBudgets", activitySpentBudgets);
        model.addAttribute("budgets", activityBudgets);
        model.addAttribute("remainingBudgets", activityRemainingBudgets);
        model.addAttribute("spentPercentageBudgets", activitySpentPercentage);

        return "calendarView";
    }

    @RequestMapping({"/testApi/{num}"})
    public String testApi(@PathVariable("num") Integer num, Model model) {
        model.addAttribute("data", activityCalendarService.getTest(num));
        return "calendarView";
    }

    @RequestMapping({"/newActivity"})
    public String activityFrom(Model model) {
        model.addAttribute("activity", new ActivityDTO());
        return "activityForm";
    }

    @RequestMapping({"/newEvent"})
    public String eventFrom(Model model) {
        model.addAttribute("activitySet", activityCalendarService.getActivities(clientEmail).keySet());
        model.addAttribute("event", new EventDTO());
        return "eventForm";
    }

    @PostMapping
    @RequestMapping({"activity"})
    public String addActivity(@ModelAttribute ActivityDTO activityDTO) {
        activityCalendarService.addActivity(clientEmail, new Activity(activityDTO.getName(), activityDTO.getBudget()));
        return "redirect:/";
    }

    @PostMapping
    @RequestMapping({"event"})
    public String addEvent(@ModelAttribute EventDTO eventDTO) {
        Long startTime = null, endTime = null;
        try {
            startTime = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm").parse(eventDTO.getStart()).getTime();
            endTime = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm").parse(eventDTO.getEnd()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Event googleEvent = new Event()
                .setSummary(eventDTO.getSummary())
                .setLocation(eventDTO.getLocation())
                .setDescription(eventDTO.getDescription())
                .setStart(new EventDateTime().setDateTime(new DateTime(startTime)))
                .setEnd(new EventDateTime().setDateTime(new DateTime(endTime)));

        activityCalendarService.addEvent(clientEmail, googleEvent, eventDTO.getActivityName());
        return "redirect:/";
    }

    //for displaying events in FullCalendar library
    class EventDTO_local {
        public String title;
        public String start;
        public String end;

        public EventDTO_local(String title, String start, String end) {
            this.title = title;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "{title='" + title + '\'' +
                    ", start='" + start + '\'' +
                    ", end='" + end + '\'' +
                    '}';
        }
    }

    //for displaying events in FullCalendar library
    private List<EventDTO_local> getEventsList() {
        Map<String, Event> activities_eventsMap = activityCalendarService.get_Activity_to_Event_Map(clientEmail);
        if (activities_eventsMap.isEmpty())
            return new ArrayList<>();

        EventDTO_local eventDTO = null;
        List<EventDTO_local> eventDTOList = new ArrayList<>(activities_eventsMap.size());
        for (Event event:activities_eventsMap.values()) {
            eventDTO = new EventDTO_local(event.getSummary()
                    , event.getStart().getDateTime().toString(), event.getEnd().getDateTime().toString());
            eventDTOList.add(eventDTO);
        }
        return eventDTOList;
    }

    /*@GetMapping("/loginSuccess")
    public String getLoginInfo(Model model, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService
                .loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());


        String userInfoEndpointUri = client.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUri();

        if (!StringUtils.isEmpty(userInfoEndpointUri)) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken()
                    .getTokenValue());
            HttpEntity entity = new HttpEntity("", headers);
            ResponseEntity<Map> response = restTemplate
                    .exchange(userInfoEndpointUri, HttpMethod.GET, entity, Map.class);
            Map userAttributes = response.getBody();
            model.addAttribute("name", userAttributes.get("name"));
            model.addAttribute("email", userAttributes.get("email"));
            clientName = (String) userAttributes.get("name");
            clientEmail = (String) userAttributes.get("email");
        }
        return "redirect:/";
    }*/
}
