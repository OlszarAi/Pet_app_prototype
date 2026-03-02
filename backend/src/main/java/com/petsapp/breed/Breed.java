package com.petsapp.breed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Encja reprezentujaca rase psa.
 *
 * <p>Rasy sa zarzadzane przez Flyway seed (V2, V3, ...). Nie tworzymy ras przez API — dane sa
 * statyczne. rarity_score (1-5): 1 = bardzo pospolita, 5 = wyjatkowo rzadka.
 */
@Entity
@Table(name = "breed")
public class Breed {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "name_pl", nullable = false, length = 100)
  private String namePl;

  @Column(name = "\"group\"", length = 50)
  private String group;

  @Column(name = "size_category", length = 20)
  private String sizeCategory;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "silhouette_url", length = 500)
  private String silhouetteUrl;

  @Column(name = "rarity_score", nullable = false)
  private short rarityScore;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  protected Breed() {}

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNamePl() {
    return namePl;
  }

  public String getGroup() {
    return group;
  }

  public String getSizeCategory() {
    return sizeCategory;
  }

  public String getDescription() {
    return description;
  }

  public String getSilhouetteUrl() {
    return silhouetteUrl;
  }

  public short getRarityScore() {
    return rarityScore;
  }

  public boolean isActive() {
    return isActive;
  }
}
