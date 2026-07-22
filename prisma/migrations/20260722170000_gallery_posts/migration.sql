CREATE TYPE "GalleryPostType" AS ENUM ('photo', 'story', 'character_sheet');
CREATE TYPE "GalleryCategory" AS ENUM ('games', 'events', 'heroes', 'tavern', 'miniatures', 'other');
CREATE TYPE "GalleryPostStatus" AS ENUM ('draft', 'published', 'hidden');

CREATE TABLE "GalleryPost" (
  "id" TEXT NOT NULL,
  "publicId" TEXT NOT NULL,
  "type" "GalleryPostType" NOT NULL DEFAULT 'photo',
  "title" TEXT NOT NULL,
  "description" TEXT,
  "storyContent" TEXT,
  "storyHtml" TEXT,
  "category" "GalleryCategory" NOT NULL DEFAULT 'games',
  "eventDate" TIMESTAMP(3),
  "authorMasterId" TEXT,
  "status" "GalleryPostStatus" NOT NULL DEFAULT 'draft',
  "isVisible" BOOLEAN NOT NULL DEFAULT true,
  "sortOrder" INTEGER NOT NULL DEFAULT 0,
  "publishedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,

  CONSTRAINT "GalleryPost_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "GalleryMedia" (
  "id" TEXT NOT NULL,
  "galleryPostId" TEXT NOT NULL,
  "fileUrl" TEXT NOT NULL,
  "thumbnailUrl" TEXT NOT NULL,
  "mediumUrl" TEXT NOT NULL,
  "width" INTEGER,
  "height" INTEGER,
  "mimeType" TEXT NOT NULL,
  "altText" TEXT,
  "sortOrder" INTEGER NOT NULL DEFAULT 0,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT "GalleryMedia_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "GalleryPost_publicId_key" ON "GalleryPost"("publicId");
CREATE INDEX "GalleryPost_status_isVisible_sortOrder_createdAt_idx" ON "GalleryPost"("status", "isVisible", "sortOrder", "createdAt");
CREATE INDEX "GalleryPost_category_status_idx" ON "GalleryPost"("category", "status");
CREATE INDEX "GalleryPost_authorMasterId_idx" ON "GalleryPost"("authorMasterId");
CREATE INDEX "GalleryMedia_galleryPostId_sortOrder_idx" ON "GalleryMedia"("galleryPostId", "sortOrder");

ALTER TABLE "GalleryPost" ADD CONSTRAINT "GalleryPost_authorMasterId_fkey" FOREIGN KEY ("authorMasterId") REFERENCES "Master"("id") ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE "GalleryMedia" ADD CONSTRAINT "GalleryMedia_galleryPostId_fkey" FOREIGN KEY ("galleryPostId") REFERENCES "GalleryPost"("id") ON DELETE CASCADE ON UPDATE CASCADE;
