# lurker-themodders

lurker-themodders is a scrapper designed to scrape the forum https://themodders.org by fetching the content and saving
it in a PostgreSQL database as pure data instead of HTML pages.

## Usage

Download release package from the [releases page](https://github.com/piotrmacha/lurker-themodders/releases). Available:

* **lurker-themodders-java21.zip** - Application compiled for Java 21+ runtime as a standalone JAR file.
* **lurker-themodders-linux-amd64.zip** - Application compiled as native image for Linux x86_64. 

  **Warning**: May contain bugs - in case of problems, use the Java version.

### How it works?

lurker-themodders is an CLI application that connects to the PostgreSQL database for storing the data and its state.
Database has `download_queue(_done|_failure)` tables which keep the list of tasks to execute. 

The app works in at least 2 stages, because first we use `build-index` command to download all boards (categories) and 
topic links, which go to the `download_queue` table. During this state no topic is actually downloaded to focus on
building a fail-safe index of all topics. Similar command is `build-new-posts` which fetches only the new posts from the 
forum using "Ostatnie wiadomości" page. 

When the first step have prepared the index, we can use `download-topics` command to download all topics from it.
This step is the most time-consuming, as it can have >10k topics to download in the queue and employs a heavy rate
limiting to not disturb the server. This step can be interrupted and resumed at any time, because a list of jobs 
is kept in the database and the job are deleted from DB only after successful download. Each account avatar and image
in the post is downloaded and stored in the `--dir` directory (default: `./assets/`).

Successful and unsuccessful jobs are stored in, respectively, `download_queue_done` and `download_queue_failure` tables.
After the download is finished, the `download_queue` should be empty, but it's possible to reschedule failed jobs using
`reschedule-failures` command and then running `download-topics` again. 

Lurker is still in development, so it may have bugs and backwards incompatible changes. You can always clear the database
completely using `clear-all` and start from scratch. 

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
    --rps 3.0  # Max requests per second. Be a good scrapper, don't overload the server.  
```

### Prepare list of new posts to download
This command fetches new posts and creates a list of topics to download. 

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar build-new-posts \
    --uri https://themodders.org/index.php?action=recent
    --rps 3.0  # Max requests per second. Be a good scrapper, don't overload the server. 
```

### Download indexed topics
This command downloads all topics that were indexed. It will take long time for a full index.

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar download-topics \
    --dir ./asset/ # Directory to store assets 
    --rps 3.0  # Max requests per second. Be a good scrapper, don't overload the server. 
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

### Reschedule Failures
This command moves all failed downloads back to the download queue.

```shell
export $(cat .env | xargs)
java -Xmx1G -jar ./lurker-themodders.jar reschedule-failures
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