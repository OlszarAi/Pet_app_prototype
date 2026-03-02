package com.petsapp.achievement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Encja reprezentujaca definicje osiagniecia (achievementu).
 *
 * <p>Dane sa seedowane przez V4__seed_achievements.sql i nie zmieniaja sie w runtime.
 * condition_type okresla rodzaj warunku: total_catches, unique_breeds, rare_catch,
 * friends_count, streak. condition_value to prog do osiagniecia.
 */
@Entity
@Table(name = "achievement")
public class Achievement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "code", nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "name_pl", nullable = false, length = 100)
  private String namePl;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "icon_url", length = 500)
  private String iconUrl;

  @Column(name = "condition_type", nullable = false, length = 50)
  private String conditionType;

  @Column(name = "condition_value", nullable = false)
  private int conditionValue;

  protected Achievement() {}

  public Integer getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getNamePl() {
    return namePl;
  }

  public String getDescription() {
    return description;
  }

  public String getIconUrl() {
    return iconUrl;
  }

  public String getConditionType() {
    return conditionType;
  }

  public int getConditionValue() {
    return conditionValue;
  }
}
