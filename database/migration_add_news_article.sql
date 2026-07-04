-- Run this script manually against the intended database.
-- The local database may already contain dbo.news_article.

IF OBJECT_ID('dbo.news_article', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.news_article (
        news_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        title NVARCHAR(255) NOT NULL,
        summary NVARCHAR(1000) NULL,
        content NVARCHAR(MAX) NULL,
        thumbnail_url NVARCHAR(2048) NULL,
        external_link NVARCHAR(2048) NULL,
        publish_date DATETIME2 NOT NULL
            CONSTRAINT DF_NEWS_ARTICLE_PUBLISH_DATE DEFAULT SYSUTCDATETIME(),
        status NVARCHAR(50) NOT NULL
            CONSTRAINT DF_NEWS_ARTICLE_STATUS DEFAULT 'published',
        created_by BIGINT NULL,
        updated_by BIGINT NULL,
        created_at DATETIME2 NOT NULL
            CONSTRAINT DF_NEWS_ARTICLE_CREATED_AT DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL,

        CONSTRAINT CK_NEWS_ARTICLE_STATUS
            CHECK (status IN ('draft', 'published', 'hidden', 'deleted')),
        CONSTRAINT FK_NEWS_ARTICLE_CREATED_BY
            FOREIGN KEY (created_by) REFERENCES dbo.[user](user_id),
        CONSTRAINT FK_NEWS_ARTICLE_UPDATED_BY
            FOREIGN KEY (updated_by) REFERENCES dbo.[user](user_id)
    );
END;
