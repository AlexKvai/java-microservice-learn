package parfenov.dev.eventmanager.events.domain;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import parfenov.dev.eventmanager.cache.EventCacheInvalidator.EventCacheInvalidation;
import parfenov.dev.eventmanager.events.db.EventEntityMapper;
import parfenov.dev.eventmanager.events.db.EventRegistrationEntity;
import parfenov.dev.eventmanager.events.db.EventRegistrationRepository;
import parfenov.dev.eventmanager.events.db.EventRepository;
import parfenov.dev.eventmanager.users.domain.User;
import java.util.List;
import java.time.LocalDateTime;
@Service
public class EventRegistrationService {
    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final EventEntityMapper eventEntityMapper;
    private final ApplicationEventPublisher appEvents;
    public EventRegistrationService(EventRegistrationRepository registrationRepository, EventRepository eventRepository,
            EventEntityMapper eventEntityMapper,ApplicationEventPublisher appEvents) {
        this.registrationRepository=registrationRepository;
        this.eventRepository=eventRepository;
        this.eventEntityMapper=eventEntityMapper;
        this.appEvents=appEvents;
    }
    @Transactional
    public void registerUserOnEvent(User user, Long eventId) {
        var event=eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(()->new EntityNotFoundException("Event not found: "+eventId));
        if(user.id().equals(event.getOwnerId()))
            throw new IllegalArgumentException("Owner cannot register on own event");
        if(event.getStatus()!=EventStatus.WAIT_START || !event.getDate().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Registration is closed");
        if(registrationRepository.findRegistration(user.id(),eventId).isPresent())
            throw new IllegalArgumentException("Already registered");
        if(event.getRegistrationList().size()>=event.getMaxPlaces())
            throw new IllegalArgumentException("No places left");
        registrationRepository.save(new EventRegistrationEntity(null,user.id(),event));
        appEvents.publishEvent(new EventCacheInvalidation(eventId));
    }
    @Transactional
    public void cancelUserRegistration(User user, Long eventId) {
        var event=eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(()->new EntityNotFoundException("Event not found: "+eventId));
        if(event.getStatus()!=EventStatus.WAIT_START)
            throw new IllegalArgumentException("Cannot cancel registration after event starts");
        var registration=registrationRepository.findRegistration(user.id(),eventId)
            .orElseThrow(()->new EntityNotFoundException("Registration not found"));
        registrationRepository.delete(registration);
        appEvents.publishEvent(new EventCacheInvalidation(eventId));
    }
    @Transactional(readOnly=true)
    public List<Event> getUserRegisteredEvents(Long userId) {
        return registrationRepository.findRegisteredEvents(userId).stream()
            .map(eventEntityMapper::toDomain).toList();
    }
}
