package parfenov.dev.eventmanager.events.domain;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import parfenov.dev.eventcommon.ChangeItem;
import parfenov.dev.eventcommon.EventChangeKafkaMessage;
import parfenov.dev.eventmanager.cache.EventCacheInvalidator.EventCacheInvalidation;
import parfenov.dev.eventmanager.events.api.*;
import parfenov.dev.eventmanager.events.db.*;
import parfenov.dev.eventmanager.locations.LocationService;
import parfenov.dev.eventmanager.users.domain.AuthenticationService;
import parfenov.dev.eventmanager.users.domain.UserRole;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final LocationService locationService;
    private final AuthenticationService authenticationService;
    private final EventEntityMapper eventEntityMapper;
    private final ApplicationEventPublisher appEvents;

    public EventService(EventRepository eventRepository, EventRegistrationRepository registrationRepository,
                        LocationService locationService, AuthenticationService authenticationService,
                        EventEntityMapper eventEntityMapper, ApplicationEventPublisher appEvents) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.locationService = locationService;
        this.authenticationService = authenticationService;
        this.eventEntityMapper = eventEntityMapper;
        this.appEvents = appEvents;
    }

    public Event createEvent(EventCreateRequestDto request) {
        var location = locationService.getLocationById(request.locationId());
        if (location.capacity() < request.maxPlaces())
            throw new IllegalArgumentException("Location capacity is below maxPlaces");
        var entity = new EventEntity(null, request.name(), authenticationService.getCurrentAuthenticatedUser().id(),
            request.maxPlaces(), new ArrayList<>(), request.date(), request.cost(), request.duration(),
            request.locationId(), EventStatus.WAIT_START);
        return eventEntityMapper.toDomain(eventRepository.save(entity));
    }

    @Cacheable(cacheNames = "events", key = "'id:' + #eventId")
    @Transactional
    public Event getEventById(Long eventId) {
        return eventEntityMapper.toDomain(eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Event not found id=" + eventId)));
    }

    @Transactional
    public void cancelEvent(Long eventId) {
        var entity = getEntity(eventId);
        checkModifyPermission(entity);
        if (entity.getStatus() == EventStatus.CANCELLED) return;
        if (entity.getStatus() == EventStatus.FINISHED || entity.getStatus() == EventStatus.STARTED)
            throw new IllegalArgumentException("Cannot cancel started or finished event");
        var before = entity.getStatus();
        entity.setStatus(EventStatus.CANCELLED);
        eventRepository.save(entity);
        publishChanged(entity, "EVENT_CANCELLED", List.of(new ChangeItem("status", before.name(), "CANCELLED")));
    }

    @Transactional
    public Event updateEvent(Long eventId, EventUpdateRequestDto update) {
        var entity = getEntity(eventId);
        checkModifyPermission(entity);
        if (entity.getStatus() != EventStatus.WAIT_START)
            throw new IllegalArgumentException("Only WAIT_START event can be updated");
        var targetLocation = update.locationId() != null ? update.locationId() : entity.getLocationId();
        var targetMaxPlaces = update.maxPlaces() != null ? update.maxPlaces() : entity.getMaxPlaces();
        if (locationService.getLocationById(targetLocation).capacity() < targetMaxPlaces)
            throw new IllegalArgumentException("Location capacity is below maxPlaces");
        if (entity.getRegistrationList().size() > targetMaxPlaces)
            throw new IllegalArgumentException("Registered users exceed new maxPlaces");

        var changes = new ArrayList<ChangeItem>();
        if (update.name() != null && !Objects.equals(entity.getName(), update.name())) {
            changes.add(new ChangeItem("name", entity.getName(), update.name())); entity.setName(update.name());
        }
        if (update.maxPlaces() != null && entity.getMaxPlaces() != update.maxPlaces()) {
            changes.add(change("maxPlaces", entity.getMaxPlaces(), update.maxPlaces())); entity.setMaxPlaces(update.maxPlaces());
        }
        if (update.date() != null && !Objects.equals(entity.getDate(), update.date())) {
            changes.add(change("date", entity.getDate(), update.date())); entity.setDate(update.date());
        }
        if (update.cost() != null && entity.getCost() != update.cost()) {
            changes.add(change("cost", entity.getCost(), update.cost())); entity.setCost(update.cost());
        }
        if (update.duration() != null && entity.getDuration() != update.duration()) {
            changes.add(change("duration", entity.getDuration(), update.duration())); entity.setDuration(update.duration());
        }
        if (update.locationId() != null && !Objects.equals(entity.getLocationId(), update.locationId())) {
            changes.add(change("locationId", entity.getLocationId(), update.locationId())); entity.setLocationId(update.locationId());
        }
        eventRepository.save(entity);
        if (!changes.isEmpty()) publishChanged(entity, "EVENT_UPDATED", changes);
        return eventEntityMapper.toDomain(entity);
    }

    private ChangeItem change(String field, Object oldValue, Object newValue) {
        return new ChangeItem(field, String.valueOf(oldValue), String.valueOf(newValue));
    }
    private EventEntity getEntity(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Event not found id=" + eventId));
    }
    private void checkModifyPermission(EventEntity entity) {
        var user = authenticationService.getCurrentAuthenticatedUser();
        if (!Objects.equals(user.id(), entity.getOwnerId()) && user.role() != UserRole.ADMIN)
            throw new AccessDeniedException("Only owner or admin can modify event");
    }
    private void publishChanged(EventEntity entity, String type, List<ChangeItem> changes) {
        var actor = authenticationService.getCurrentAuthenticatedUser();
        appEvents.publishEvent(new EventCacheInvalidation(entity.getId()));
        appEvents.publishEvent(new EventChangeKafkaMessage(
            UUID.randomUUID(), type, entity.getId(), LocalDateTime.now(), entity.getOwnerId(),
            actor.id(), registrationRepository.findSubscribers(entity.getId()), changes, entity.getName()));
    }
    @Transactional(readOnly = true)
    public List<Event> searchByFilter(EventSearchFilter filter) {
        return eventRepository.findEvents(filter.name(), filter.placesMin(), filter.placesMax(),
            filter.dateStartAfter(), filter.dateStartBefore(), filter.costMin(), filter.costMax(),
            filter.durationMin(), filter.durationMax(), filter.locationId(), filter.eventStatus())
            .stream().map(eventEntityMapper::toDomain).toList();
    }
    @Transactional(readOnly = true)
    public List<Event> getCurrentUserEvents() {
        return eventRepository.findAllByOwnerIdIs(authenticationService.getCurrentAuthenticatedUser().id())
            .stream().map(eventEntityMapper::toDomain).toList();
    }
}
