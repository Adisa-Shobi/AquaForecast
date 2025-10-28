"""Reset database - drops all tables and alembic version."""
import os
import psycopg2

def reset_database():
    """Drop all tables and reset alembic version."""
    db_url = os.getenv('DATABASE_URL')
    if not db_url:
        print("❌ DATABASE_URL not found in environment")
        return

    print(f"🔄 Connecting to database...")
    conn = psycopg2.connect(db_url)
    conn.autocommit = True
    cursor = conn.cursor()

    try:
        # Drop all tables
        print("🗑️  Dropping existing tables...")
        cursor.execute("DROP TABLE IF EXISTS alembic_version CASCADE;")
        cursor.execute("DROP TABLE IF EXISTS model_versions CASCADE;")
        cursor.execute("DROP TABLE IF EXISTS farm_data CASCADE;")
        cursor.execute("DROP TABLE IF EXISTS users CASCADE;")

        # Drop indexes if they exist
        cursor.execute("DROP INDEX IF EXISTS idx_farm_data_location;")
        cursor.execute("DROP INDEX IF EXISTS idx_farm_data_country_code;")
        cursor.execute("DROP INDEX IF EXISTS idx_farm_data_start_date;")
        cursor.execute("DROP INDEX IF EXISTS idx_farm_data_recorded_at;")
        cursor.execute("DROP INDEX IF EXISTS idx_farm_data_user_id;")
        cursor.execute("DROP INDEX IF EXISTS idx_users_email;")
        cursor.execute("DROP INDEX IF EXISTS idx_users_firebase_uid;")
        cursor.execute("DROP INDEX IF EXISTS idx_model_versions_version;")

        print("✅ Database reset successfully!")
        print("\nNow run: alembic upgrade head")

    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    reset_database()
