package parfenov.dev.eventmanager.events.api;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import parfenov.dev.eventmanager.events.domain.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final static Logger log =  LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final EventDtoMapper eventDtoMapper;

    public EventController(EventService eventService, EventDtoMapper eventDtoMapper) {
        this.eventService = eventService;
        this.eventDtoMapper = eventDtoMapper;
    }

    @PostMapping
    public ResponseEntity<EventDto> createEvent(
            @RequestBody @Valid EventCreateRequestDto eventCreateRequestDto
    ) {
        log.info("Get request for create new event: request={}", eventCreateRequestDto);
        var createdEvent = eventService.createEvent(eventCreateRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(eventDtoMapper.toDto(createdEvent));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEventById(
            @PathVariable Long eventId
    ) {
        log.info("Get request for get event by id: request={}", eventId);

        var event = eventService.getEventById(eventId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(eventDtoMapper.toDto(event));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> cancelEvent(
            @PathVariable Long eventId
    ) {
        log.info("Get request for cancel event by id: request={}", eventId);

        eventService.cancelEvent(eventId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable("eventId") Long eventId,
            @RequestBody @Valid EventUpdateRequestDto eventUpdateRequestDto
    ) {
        log.info("Get request for update event by id={}: request={}", eventId, eventUpdateRequestDto);

        var updatedEvent = eventService.updateEvent(eventId, eventUpdateRequestDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(eventDtoMapper.toDto(updatedEvent));
    }

    @PostMapping("/search")
    public ResponseEntity<List<EventDto>> searchEvents(
            @RequestBody @Valid EventSearchFilter searchFilter
    ) {
        log.info("Get request for search events: request={}", searchFilter);

        var foundEvents = eventService.searchByFilter(searchFilter);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(foundEvents.stream()
                        .map(eventDtoMapper::toDto)
                        .toList()
                );
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventDto>> getUserEvents() {
        log.info("Get request for get user events");

        var events = eventService.getCurrentUserEvents();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(events.stream()
                        .map(eventDtoMapper::toDto)
                        .toList()
                );
    }

}
