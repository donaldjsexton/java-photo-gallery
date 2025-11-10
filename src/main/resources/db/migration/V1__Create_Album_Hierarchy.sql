-- V1__Create_Album_Hierarchy.sql
-- Create album hierarchy tables for professional photography gallery

-- Create albums table
CREATE TABLE albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    album_type VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    client_name VARCHAR(200),
    shoot_date TIMESTAMP,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sort_order INT NOT NULL DEFAULT 0,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    is_client_visible BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    cover_photo_id BIGINT,
    slug VARCHAR(500),

    CONSTRAINT fk_album_parent FOREIGN KEY (parent_id) REFERENCES albums(id) ON DELETE SET NULL,
    CONSTRAINT uk_album_slug UNIQUE (slug)
);

-- Add album-related columns to photos table
ALTER TABLE photos
ADD COLUMN album_id BIGINT,
ADD COLUMN workflow_status VARCHAR(50) NOT NULL DEFAULT 'RAW',
ADD COLUMN sort_order_in_album INT NOT NULL DEFAULT 0,
ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN client_approved BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN is_portfolio_image BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN client_notes TEXT,
ADD COLUMN internal_notes TEXT;

-- Add foreign key constraint from photos to albums
ALTER TABLE photos
ADD CONSTRAINT fk_photo_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE SET NULL;

-- Create indexes for better performance
CREATE INDEX idx_albums_parent_id ON albums(parent_id);
CREATE INDEX idx_albums_album_type ON albums(album_type);
CREATE INDEX idx_albums_client_name ON albums(client_name);
CREATE INDEX idx_albums_shoot_date ON albums(shoot_date);
CREATE INDEX idx_albums_sort_order ON albums(sort_order);
CREATE INDEX idx_albums_is_public ON albums(is_public);
CREATE INDEX idx_albums_is_featured ON albums(is_featured);
CREATE INDEX idx_albums_slug ON albums(slug);

CREATE INDEX idx_photos_album_id ON photos(album_id);
CREATE INDEX idx_photos_workflow_status ON photos(workflow_status);
CREATE INDEX idx_photos_sort_order_in_album ON photos(sort_order_in_album);
CREATE INDEX idx_photos_is_featured ON photos(is_featured);
CREATE INDEX idx_photos_client_approved ON photos(client_approved);
CREATE INDEX idx_photos_is_portfolio_image ON photos(is_portfolio_image);

-- Insert default root albums for professional photography structure
INSERT INTO albums (name, album_type, sort_order, is_public, slug) VALUES
('Clients', 'CLIENT', 1, FALSE, 'clients'),
('Collections', 'COLLECTION', 2, TRUE, 'collections'),
('Portfolio', 'PORTFOLIO', 3, TRUE, 'portfolio'),
('Events by Type', 'EVENT_TYPE', 4, FALSE, 'events-by-type'),
('Archive', 'ARCHIVE', 5, FALSE, 'archive');

-- Insert event type sub-albums
INSERT INTO albums (name, album_type, parent_id, sort_order, is_public, slug) VALUES
('Weddings', 'EVENT_TYPE', (SELECT id FROM albums WHERE slug = 'events-by-type'), 1, TRUE, 'weddings'),
('Portraits', 'EVENT_TYPE', (SELECT id FROM albums WHERE slug = 'events-by-type'), 2, TRUE, 'portraits'),
('Corporate', 'EVENT_TYPE', (SELECT id FROM albums WHERE slug = 'events-by-type'), 3, TRUE, 'corporate'),
('Events', 'EVENT_TYPE', (SELECT id FROM albums WHERE slug = 'events-by-type'), 4, TRUE, 'events');

-- Insert featured collections
INSERT INTO albums (name, album_type, parent_id, sort_order, is_public, is_featured, slug) VALUES
('Best of 2024', 'COLLECTION', (SELECT id FROM albums WHERE slug = 'collections'), 1, TRUE, TRUE, 'best-of-2024'),
('Featured Weddings', 'COLLECTION', (SELECT id FROM albums WHERE slug = 'collections'), 2, TRUE, TRUE, 'featured-weddings'),
('Portfolio Highlights', 'COLLECTION', (SELECT id FROM albums WHERE slug = 'portfolio'), 1, TRUE, TRUE, 'portfolio-highlights');
