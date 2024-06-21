# lurker-themodders

lurker-themodders is a scrapper designed to scrape the forum https://themodders.org by fetching the content and saving
it in a PostgreSQL database as pure data instead of HTML pages.

## Usage

Download release package from the [releases page](https://github.com/piotrmacha/lurker-themodders/releases). Available:

* **lurker-themodders-java21.zip** - Application compiled for Java 21+ runtime as a standalone JAR file.
* **lurker-themodders-linux-amd64.zip** - Application compiled as native image for Linux x86_64. 

  **Warning**: May contain bugs - in case of problems, use the Java version. 

### Start Docker PostgreSQL

You can change the database credentials and port in `.env` file.

```shell
docker compose up -d
````

### Prepare list of all topics to download

This command fetches all the boards and creates a list of topics to download. 

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar build-index \
    --uri https://themodders.org 
```

### Prepare list of new posts to download
This command fetches new posts and creates a list of topics to download. 

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar build-new-posts \
    --uri https://themodders.org/index.php?action=recent 
```

### Download indexed topics
This command downloads all topics that were indexed. It will take long time for a full index.

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar download-topics \
    --dir ./asset/ # Directory to store assets 
```

### Clear download queue
This command clears the download queue.

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar clear-download-queue
```

### Clear all
This command clears all tables in the database. It will delete everything and leave the DB in clean state. 

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar clear-all
```

## Development

For development purposes, you can use the provided docker-compose file to start a PostgreSQL.

Copy the example environment file to a `.env` file.

```shell
cp example.env .env
```

Then, start the PostgreSQL database.

```shell
docker-compose up -d
```

Migrate the database and generate the JOOQ classes if you have changed the schema.

```shell
./gradlew flywayMigrate
./gradlew generateJooq
```

To run the project you can use the following command:

```shell
source .env
./gradlew bootRun --args='build-index'
./gradlew bootRun --args='build-new-posts'
./gradlew bootRun --args='download-topics'
```

If you are using other tools (like IntelliJ IDEA), you have to include the environment variables from the .env file.

## License

The project is licensed under [MIT license](./LICENSE.md).