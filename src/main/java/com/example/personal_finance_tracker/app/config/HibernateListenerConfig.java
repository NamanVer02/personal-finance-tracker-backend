package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.services.LoggingService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@RequiredArgsConstructor
public class HibernateListenerConfig {

    private final EntityManagerFactory emf;
    private final LoggingService loggingService;

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(new PostInsertEventListener() {
            @Override
            public void onPostInsert(PostInsertEvent event) {
                logIfNeeded(event.getEntity(), "CREATE", getEntityId(event.getId()), "Entity created");
            }

            @Override
            public boolean requiresPostCommitHandling(EntityPersister persister) {
                return false;
            }
        });

        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(new PostUpdateEventListener() {
            @Override
            public void onPostUpdate(PostUpdateEvent event) {
                logIfNeeded(event.getEntity(), "UPDATE", getEntityId(event.getId()), "Entity updated");
            }

            @Override
            public boolean requiresPostCommitHandling(EntityPersister persister) {
                return false;
            }
        });

        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(new PostDeleteEventListener() {
            @Override
            public void onPostDelete(PostDeleteEvent event) {
                logIfNeeded(event.getEntity(), "DELETE", getEntityId(event.getId()), "Entity deleted");
            }

            @Override
            public boolean requiresPostCommitHandling(EntityPersister persister) {
                return false;
            }
        });
    }

    private void logIfNeeded(Object entity, String method, String entityId, String description) {
        if (entity.getClass().isAnnotationPresent(Loggable.class)) {
            String username = getCurrentUsername();
            String entityName = entity.getClass().getSimpleName();

            loggingService.logDatabaseEvent(username, method, entityName, entityId, description);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    private String getEntityId(Object id) {
        return id != null ? id.toString() : "unknown";
    }
}