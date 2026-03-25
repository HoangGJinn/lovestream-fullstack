package com.hcmute.lovestream.dto.response;

public record WatchRoomCardResponse(
		String roomName,
		String roomCode,
		boolean privateRoom,
		String movieId,
		String movieTitle,
		String moviePosterUrl,
		String category,
		String statusText,
		boolean live,
		long participantCount,
		int maxParticipants,
		String hostName,
		String createdAtLabel
) {
}

