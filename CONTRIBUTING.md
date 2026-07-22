# Contributing

## Рабочий процесс

1. Создайте ветку от `main`.
2. Используйте понятное имя ветки: `feat/...`, `fix/...`, `docs/...`, `chore/...`.
3. Не коммитьте `.env`, токены, базу, uploads, логи и резервные копии.
4. Перед pull request выполните:

```bash
pnpm install
pnpm check
pnpm build
```

## Коммиты

Используйте короткие сообщения в стиле:

- `feat(rating): add public leaderboard`
- `fix(bot): handle polling startup errors`
- `docs(project): describe local setup`

## База данных

Все изменения схемы проходят через Prisma migrations. Не изменяйте production-базу вручную без миграции и резервной копии.

## Персональные данные

Не добавляйте реальные заявки, телефоны, Telegram-переписки и приватные ID в fixtures, seed или документацию.
