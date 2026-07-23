package by.taverna.shlyapnika.bot.telegram;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GameDraft {
  private String displayName;
  private String contactUrl;
  private String title;
  private String date;
  private String time;
  private Integer durationMinutes;
  private String description;
  private Integer minPlayers;
  private Integer maxPlayers;
  private BigDecimal price;
  private String currency = "BYN";
  private String gameSystem;
  private String experienceLevel;
  private String ageRating;
  private String contactOverride;
  private String galleryType;
  private String galleryTitle;
  private String galleryDescription;
  private String galleryStoryContent;
  private String galleryStoryHtml;
  private String galleryCategory;
  private String galleryEventDate;
  private List<GalleryMediaDraft> galleryMedia = new ArrayList<>();
  private String ratingDisplayName;

  public String displayName() { return displayName; }
  public String getDisplayName() { return displayName; }
  public void displayName(String value) { displayName = value; }
  public void setDisplayName(String value) { displayName = value; }
  public String contactUrl() { return contactUrl; }
  public String getContactUrl() { return contactUrl; }
  public void contactUrl(String value) { contactUrl = value; }
  public void setContactUrl(String value) { contactUrl = value; }
  public String title() { return title; }
  public String getTitle() { return title; }
  public void title(String value) { title = value; }
  public void setTitle(String value) { title = value; }
  public String date() { return date; }
  public String getDate() { return date; }
  public void date(String value) { date = value; }
  public void setDate(String value) { date = value; }
  public String time() { return time; }
  public String getTime() { return time; }
  public void time(String value) { time = value; }
  public void setTime(String value) { time = value; }
  public Integer durationMinutes() { return durationMinutes; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void durationMinutes(Integer value) { durationMinutes = value; }
  public void setDurationMinutes(Integer value) { durationMinutes = value; }
  public String description() { return description; }
  public String getDescription() { return description; }
  public void description(String value) { description = value; }
  public void setDescription(String value) { description = value; }
  public Integer minPlayers() { return minPlayers; }
  public Integer getMinPlayers() { return minPlayers; }
  public void minPlayers(Integer value) { minPlayers = value; }
  public void setMinPlayers(Integer value) { minPlayers = value; }
  public Integer maxPlayers() { return maxPlayers; }
  public Integer getMaxPlayers() { return maxPlayers; }
  public void maxPlayers(Integer value) { maxPlayers = value; }
  public void setMaxPlayers(Integer value) { maxPlayers = value; }
  public BigDecimal price() { return price; }
  public BigDecimal getPrice() { return price; }
  public void price(BigDecimal value) { price = value; }
  public void setPrice(BigDecimal value) { price = value; }
  public String currency() { return currency; }
  public String getCurrency() { return currency; }
  public void currency(String value) { currency = value; }
  public void setCurrency(String value) { currency = value; }
  public String gameSystem() { return gameSystem; }
  public String getGameSystem() { return gameSystem; }
  public void gameSystem(String value) { gameSystem = value; }
  public void setGameSystem(String value) { gameSystem = value; }
  public String experienceLevel() { return experienceLevel; }
  public String getExperienceLevel() { return experienceLevel; }
  public void experienceLevel(String value) { experienceLevel = value; }
  public void setExperienceLevel(String value) { experienceLevel = value; }
  public String ageRating() { return ageRating; }
  public String getAgeRating() { return ageRating; }
  public void ageRating(String value) { ageRating = value; }
  public void setAgeRating(String value) { ageRating = value; }
  public String contactOverride() { return contactOverride; }
  public String getContactOverride() { return contactOverride; }
  public void contactOverride(String value) { contactOverride = value; }
  public void setContactOverride(String value) { contactOverride = value; }
  public String galleryType() { return galleryType; }
  public String getGalleryType() { return galleryType; }
  public void galleryType(String value) { galleryType = value; }
  public void setGalleryType(String value) { galleryType = value; }
  public String galleryTitle() { return galleryTitle; }
  public String getGalleryTitle() { return galleryTitle; }
  public void galleryTitle(String value) { galleryTitle = value; }
  public void setGalleryTitle(String value) { galleryTitle = value; }
  public String galleryDescription() { return galleryDescription; }
  public String getGalleryDescription() { return galleryDescription; }
  public void galleryDescription(String value) { galleryDescription = value; }
  public void setGalleryDescription(String value) { galleryDescription = value; }
  public String galleryStoryContent() { return galleryStoryContent; }
  public String getGalleryStoryContent() { return galleryStoryContent; }
  public void galleryStoryContent(String value) { galleryStoryContent = value; }
  public void setGalleryStoryContent(String value) { galleryStoryContent = value; }
  public String galleryStoryHtml() { return galleryStoryHtml; }
  public String getGalleryStoryHtml() { return galleryStoryHtml; }
  public void galleryStoryHtml(String value) { galleryStoryHtml = value; }
  public void setGalleryStoryHtml(String value) { galleryStoryHtml = value; }
  public String galleryCategory() { return galleryCategory; }
  public String getGalleryCategory() { return galleryCategory; }
  public void galleryCategory(String value) { galleryCategory = value; }
  public void setGalleryCategory(String value) { galleryCategory = value; }
  public String galleryEventDate() { return galleryEventDate; }
  public String getGalleryEventDate() { return galleryEventDate; }
  public void galleryEventDate(String value) { galleryEventDate = value; }
  public void setGalleryEventDate(String value) { galleryEventDate = value; }
  public List<GalleryMediaDraft> galleryMedia() { return galleryMedia; }
  public List<GalleryMediaDraft> getGalleryMedia() { return galleryMedia; }
  public void galleryMedia(List<GalleryMediaDraft> value) { galleryMedia = value == null ? new ArrayList<>() : value; }
  public void setGalleryMedia(List<GalleryMediaDraft> value) { galleryMedia(value); }
  public String ratingDisplayName() { return ratingDisplayName; }
  public String getRatingDisplayName() { return ratingDisplayName; }
  public void ratingDisplayName(String value) { ratingDisplayName = value; }
  public void setRatingDisplayName(String value) { ratingDisplayName = value; }

  public record GalleryMediaDraft(
      String fileUrl,
      String thumbnailUrl,
      String mediumUrl,
      Integer width,
      Integer height,
      String mimeType,
      String altText,
      Integer sortOrder
  ) {
  }
}
