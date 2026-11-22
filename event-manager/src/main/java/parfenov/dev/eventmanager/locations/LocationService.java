package parfenov.dev.eventmanager.locations;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.List;

@Service
public class LocationService {
    private final LocationRepository repository;
    private final LocationEntityMapper entityMapper;

    public LocationService(
            LocationRepository repository,
            LocationEntityMapper entityMapper
    ) {
        this.repository = repository;
        this.entityMapper = entityMapper;
    }

    @Cacheable(cacheNames = "locations", key = "'all'")
    public List<Location> getAllLocations() {
        return repository.findAll()
                .stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @CacheEvict(cacheNames = "locations", key = "'all'")
    public Location createLocation(Location locationToCreate) {
        if (locationToCreate.id() != null) {
            throw new IllegalArgumentException("Can not create location with provided id. Id must be empty");
        }
        var createdLocation = repository.save(entityMapper.toEntity(locationToCreate));
        return entityMapper.toDomain(createdLocation);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "locations", key = "'all'"),
        @CacheEvict(cacheNames = "locations", key = "'id:' + #locationId")
    })
    public Location updateLocation(
            Location locationToUpdate,
            Long locationId
    ) {
        if (locationToUpdate.id() != null) {
            throw new IllegalArgumentException("Can not update location with provided id. Id must be empty");
        }
        var entityToUpdate = repository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Location entity wasn't found with provided id=%s".formatted(locationId))
                );
        entityToUpdate.setAddress(locationToUpdate.address());
        entityToUpdate.setDescription(locationToUpdate.description());
        entityToUpdate.setCapacity(locationToUpdate.capacity());
        entityToUpdate.setName(locationToUpdate.name());

        var updatedEntity = repository.save(entityToUpdate);

        return entityMapper.toDomain(updatedEntity);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "locations", key = "'all'"),
        @CacheEvict(cacheNames = "locations", key = "'id:' + #locationId")
    })
    public Location deleteLocation(Long locationId) {
        var entityToDelete = repository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Location entity wasn't found with provided id=%s".formatted(locationId))
                );
        repository.deleteById(locationId);
        return entityMapper.toDomain(entityToDelete);
    }

    @Cacheable(cacheNames = "locations", key = "'id:' + #locationId")
    public Location getLocationById(Long locationId) {
        var foundEntity = repository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Location entity wasn't found with provided id=%s".formatted(locationId))
                );
        return entityMapper.toDomain(foundEntity);
    }
}
