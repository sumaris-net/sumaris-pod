package net.sumaris.core.model.referential;

public enum UserProfileEnum {

  ADMIN(1, "ADMIN"),
  USER(2, "USER"),
  SUPERVISOR(3, "SUPERVISOR"),
  GUEST(4, "GUEST");

  public final int id;
  public final String label;

  UserProfileEnum(int id, String label) {
      this.id = id;
      this.label = label;
  }
}
