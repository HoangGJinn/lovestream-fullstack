package com.hcmute.lovestream.service.watchtogether;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomSessionTracker {

    private final Map<String, SessionInfo> sessionInfoById = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<String>>> roomUserSessions = new ConcurrentHashMap<>();

    public synchronized boolean registerSession(String sessionId, String roomCode, String userEmail) {
        if (sessionId == null || roomCode == null || userEmail == null) {
            return false;
        }

        SessionInfo previous = sessionInfoById.put(sessionId, new SessionInfo(roomCode, userEmail));
        if (previous != null) {
            removeSessionReference(sessionId, previous.roomCode(), previous.userEmail());
        }

        Set<String> sessions = roomUserSessions
                .computeIfAbsent(roomCode, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(userEmail, key -> ConcurrentHashMap.newKeySet());

        sessions.add(sessionId);
        return sessions.size() == 1;
    }

    public synchronized Optional<DisconnectInfo> unregisterSession(String sessionId) {
        SessionInfo info = sessionInfoById.remove(sessionId);
        if (info == null) {
            return Optional.empty();
        }

        boolean lastSessionForUser = removeSessionReference(sessionId, info.roomCode(), info.userEmail());
        return Optional.of(new DisconnectInfo(info.roomCode(), info.userEmail(), lastSessionForUser));
    }

    private boolean removeSessionReference(String sessionId, String roomCode, String userEmail) {
        Map<String, Set<String>> userSessions = roomUserSessions.get(roomCode);
        if (userSessions == null) {
            return true;
        }

        Set<String> sessions = userSessions.get(userEmail);
        if (sessions == null) {
            return true;
        }

        sessions.remove(sessionId);
        boolean lastSessionForUser = sessions.isEmpty();

        if (lastSessionForUser) {
            userSessions.remove(userEmail);
        }
        if (userSessions.isEmpty()) {
            roomUserSessions.remove(roomCode);
        }

        return lastSessionForUser;
    }

    private record SessionInfo(String roomCode, String userEmail) {
    }

    public record DisconnectInfo(String roomCode, String userEmail, boolean lastSessionForUserInRoom) {
    }
}
