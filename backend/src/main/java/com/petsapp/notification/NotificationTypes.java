package com.petsapp.notification;

/**
 * Stale definiujace typy powiadomien uzywane w polu {@code type} encji Notification.
 *
 * <p>Kazdy typ mapuje sie na odpowiednie ustawienie push w UserSettings:
 * <ul>
 *   <li>LIKE, BATCH_LIKES → push_likes
 *   <li>COMMENT → push_comments
 *   <li>FRIEND_REQUEST, FRIEND_ACCEPTED → push_friend_requests
 *   <li>ACHIEVEMENT → push_achievements
 * </ul>
 */
public final class NotificationTypes {

  private NotificationTypes() {}

  public static final String LIKE = "LIKE";
  public static final String BATCH_LIKES = "BATCH_LIKES";
  public static final String COMMENT = "COMMENT";
  public static final String FRIEND_REQUEST = "FRIEND_REQUEST";
  public static final String FRIEND_ACCEPTED = "FRIEND_ACCEPTED";
  public static final String ACHIEVEMENT = "ACHIEVEMENT";
}
