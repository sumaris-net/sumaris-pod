package net.sumaris.core.model.administration.programStrategy;

public enum AcquisitionLevelEnum {

  TRIP(1, "TRIP"),
  SURVIVAL_TEST(7, "SURVIVAL_TEST"),
  INDIVIDUAL_MONITORING(8, "INDIVIDUAL_MONITORING");

  public final int id;
  public final String label;

  AcquisitionLevelEnum(int id, String label) {
      this.id = id;
      this.label = label;
  }
}
