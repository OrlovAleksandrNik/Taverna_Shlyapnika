import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { extname, join, resolve } from "node:path";
import sharp from "sharp";
import { config } from "../config.js";
import { prisma } from "../db.js";
import { logger } from "../logger.js";
import { audit } from "./audit.js";

const categories = ["games", "events", "heroes", "tavern", "miniatures", "other"] as const;
const statuses = ["draft", "published", "hidden"] as const;

type GalleryCategory = typeof categories[number];
type GalleryStatus = typeof statuses[number];
type GalleryPostType = "photo" | "story" | "character_sheet";

type StoredMedia = {
  fileUrl: string;
  thumbnailUrl: string;
  mediumUrl: string;
  width?: number;
  height?: number;
  mimeType: string;
  altText?: string;
  sortOrder?: number;
};

type TelegramFileApi = {
  getFile(fileId: string): Promise<{ file_path?: string }>;
};

export const galleryCategories = categories;

export function createGalleryPublicId() {
  return `gal_${randomUUID().replace(/-/g, "").slice(0, 18)}`;
}

function publicUploadUrl(relativePath: string) {
  return `${config.PUBLIC_UPLOADS_URL.replace(/\/$/, "")}/${relativePath.replace(/\\/g, "/").replace(/^\/+/, "")}`;
}

function contentStoragePath(relativePath: string) {
  return resolve(process.cwd(), config.FILE_STORAGE_DIR, relativePath);
}

function detectImage(buffer: Buffer) {
  if (buffer.length > 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff) {
    return { mimeType: "image/jpeg", ext: ".jpg" };
  }

  if (
    buffer.length > 24 &&
    buffer[0] === 0x89 &&
    buffer.toString("ascii", 1, 4) === "PNG" &&
    buffer[12] === 0x49 &&
    buffer[13] === 0x48 &&
    buffer[14] === 0x44 &&
    buffer[15] === 0x52
  ) {
    return { mimeType: "image/png", ext: ".png" };
  }

  if (buffer.length > 12 && buffer.toString("ascii", 0, 4) === "RIFF" && buffer.toString("ascii", 8, 12) === "WEBP") {
    return { mimeType: "image/webp", ext: ".webp" };
  }

  return null;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

export function formatGalleryStory(input?: string | null) {
  const text = (input || "").trim();
  if (!text) return null;

  const blocks = text.split(/\n{2,}/).map((block) => block.trim()).filter(Boolean);
  return blocks.map((block) => {
    const escaped = escapeHtml(block);
    if (/^#{2,3}\s+/.test(block)) return `<h3>${escaped.replace(/^#{2,3}\s+/, "")}</h3>`;
    if (/^>\s+/.test(block)) return `<blockquote>${escaped.replace(/^&gt;\s+/, "")}</blockquote>`;
    if (/^[-*]\s+/m.test(block)) {
      const items = block.split(/\n/).map((line) => line.replace(/^[-*]\s+/, "").trim()).filter(Boolean);
      return `<ul>${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`;
    }
    return `<p>${escaped.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>").replace(/\*([^*]+)\*/g, "<em>$1</em>").replace(/\n/g, "<br>")}</p>`;
  }).join("");
}

export async function storeTelegramGalleryImage(input: {
  api: TelegramFileApi;
  fileId: string;
  postPublicId: string;
  altText?: string;
  sortOrder?: number;
}) {
  const file = await input.api.getFile(input.fileId);
  if (!file.file_path) throw new Error("Telegram не вернул путь к файлу.");

  const response = await fetch(`https://api.telegram.org/file/bot${config.TELEGRAM_BOT_TOKEN}/${file.file_path}`);
  if (!response.ok) throw new Error(`Не удалось скачать файл из Telegram: ${response.status}`);

  const arrayBuffer = await response.arrayBuffer();
  const buffer = Buffer.from(arrayBuffer);
  if (buffer.length > 12 * 1024 * 1024) throw new Error("Файл слишком большой. Максимум 12 МБ.");

  const detected = detectImage(buffer);
  if (!detected) throw new Error("Поддерживаются только настоящие JPEG, PNG или WEBP изображения.");

  const image = sharp(buffer, { failOn: "warning" }).rotate();
  const metadata = await image.metadata();
  const width = metadata.width;
  const height = metadata.height;

  const originalExt = extname(file.file_path).toLowerCase();
  const ext = [".jpg", ".jpeg", ".png", ".webp"].includes(originalExt) ? originalExt.replace(".jpeg", ".jpg") : detected.ext;
  const baseName = randomUUID().replace(/-/g, "");
  const name = `${baseName}${ext}`;
  const mediumName = `${baseName}-medium.webp`;
  const thumbnailName = `${baseName}-thumb.webp`;
  const relativeDir = join("gallery", input.postPublicId);
  const relativePath = join(relativeDir, name);
  const mediumPath = join(relativeDir, mediumName);
  const thumbnailPath = join(relativeDir, thumbnailName);
  const absoluteDir = contentStoragePath(relativeDir);
  const absolutePath = contentStoragePath(relativePath);
  await mkdir(absoluteDir, { recursive: true });
  await writeFile(absolutePath, buffer);
  await writeFile(
    contentStoragePath(mediumPath),
    await sharp(buffer).rotate().resize({ width: 1400, height: 1400, fit: "inside", withoutEnlargement: true }).webp({ quality: 84 }).toBuffer()
  );
  await writeFile(
    contentStoragePath(thumbnailPath),
    await sharp(buffer).rotate().resize({ width: 720, height: 540, fit: "cover", position: "attention" }).webp({ quality: 78 }).toBuffer()
  );

  const url = publicUploadUrl(relativePath);
  return {
    fileUrl: url,
    thumbnailUrl: publicUploadUrl(thumbnailPath),
    mediumUrl: publicUploadUrl(mediumPath),
    width,
    height,
    mimeType: detected.mimeType,
    altText: input.altText,
    sortOrder: input.sortOrder || 0
  } satisfies StoredMedia;
}

function postDto(post: any) {
  return {
    id: post.id,
    publicId: post.publicId,
    type: post.type,
    title: post.title,
    description: post.description,
    storyHtml: post.storyHtml,
    category: post.category,
    eventDate: post.eventDate?.toISOString() || null,
    master: post.authorMaster ? { id: post.authorMaster.id, name: post.authorMaster.displayName } : null,
    media: (post.media || []).map((item: any) => ({
      id: item.id,
      fileUrl: item.fileUrl,
      thumbnailUrl: item.thumbnailUrl,
      mediumUrl: item.mediumUrl,
      width: item.width,
      height: item.height,
      mimeType: item.mimeType,
      altText: item.altText
    })),
    createdAt: post.createdAt?.toISOString() || null,
    publishedAt: post.publishedAt?.toISOString() || null
  };
}

export async function listPublicGalleryPosts(query: { category?: string; limit?: number; offset?: number } = {}) {
  const category = categories.includes(query.category as GalleryCategory) ? query.category as GalleryCategory : undefined;
  const posts = await prisma.galleryPost.findMany({
    where: {
      status: "published",
      isVisible: true,
      ...(category ? { category } : {})
    },
    include: {
      authorMaster: { select: { id: true, displayName: true } },
      media: { orderBy: { sortOrder: "asc" } }
    },
    orderBy: [{ sortOrder: "asc" }, { eventDate: "desc" }, { createdAt: "desc" }],
    take: Math.min(query.limit || 60, 100),
    skip: query.offset || 0
  });

  logger.info({ count: posts.length, category }, "public gallery requested");
  return posts.map(postDto);
}

export async function getPublicGalleryPost(publicId: string) {
  const post = await prisma.galleryPost.findFirst({
    where: { publicId, status: "published", isVisible: true },
    include: {
      authorMaster: { select: { id: true, displayName: true } },
      media: { orderBy: { sortOrder: "asc" } }
    }
  });
  return post ? postDto(post) : null;
}

export async function createGalleryPost(input: {
  type: GalleryPostType;
  title: string;
  description?: string;
  storyContent?: string;
  category: GalleryCategory;
  eventDate?: Date;
  authorMasterId?: string;
  status: GalleryStatus;
  media?: StoredMedia[];
  createdByTelegramId?: bigint;
  publicId?: string;
}) {
  const publicId = input.publicId || createGalleryPublicId();
  const post = await prisma.galleryPost.create({
    data: {
      publicId,
      type: input.type,
      title: input.title.trim(),
      description: input.description?.trim() || null,
      storyContent: input.storyContent?.trim() || null,
      storyHtml: formatGalleryStory(input.storyContent),
      category: input.category,
      eventDate: input.eventDate || null,
      authorMasterId: input.authorMasterId || null,
      status: input.status,
      publishedAt: input.status === "published" ? new Date() : null,
      media: input.media?.length
        ? {
            create: input.media.map((media, index) => ({
              fileUrl: media.fileUrl,
              thumbnailUrl: media.thumbnailUrl,
              mediumUrl: media.mediumUrl,
              width: media.width,
              height: media.height,
              mimeType: media.mimeType,
              altText: media.altText,
              sortOrder: media.sortOrder ?? index
            }))
          }
        : undefined
    },
    include: {
      authorMaster: { select: { id: true, displayName: true } },
      media: { orderBy: { sortOrder: "asc" } }
    }
  });

  await audit(input.createdByTelegramId?.toString(), "gallery.post_created", "GalleryPost", post.id, {
    publicId: post.publicId,
    status: post.status,
    type: post.type
  });
  logger.info({ postId: post.id, publicId: post.publicId, status: post.status }, "gallery post created");
  return postDto(post);
}

export async function listGalleryPostsForBot(masterId: string, isAdmin: boolean) {
  return prisma.galleryPost.findMany({
    where: isAdmin ? {} : { authorMasterId: masterId },
    include: { media: true },
    orderBy: { createdAt: "desc" },
    take: 12
  });
}

export async function setGalleryPostStatus(input: {
  postId: string;
  masterId: string;
  isAdmin: boolean;
  status: GalleryStatus;
  createdByTelegramId?: bigint;
}) {
  const post = await prisma.galleryPost.findFirst({
    where: input.isAdmin ? { id: input.postId } : { id: input.postId, authorMasterId: input.masterId }
  });
  if (!post) throw new Error("Публикация не найдена или принадлежит другому мастеру.");

  const next = await prisma.galleryPost.update({
    where: { id: post.id },
    data: {
      status: input.status,
      isVisible: input.status !== "hidden",
      publishedAt: input.status === "published" && !post.publishedAt ? new Date() : post.publishedAt
    }
  });
  await audit(input.createdByTelegramId?.toString(), `gallery.post_${input.status}`, "GalleryPost", post.id);
  return next;
}
