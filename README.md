# Game progress API

# Instructions how to use Docker

1) Clone repositories with API and DB code so clone this one and from the link https://github.com/KatTihanovich/game_progress_db
2) Use .env.example file to create .env and fill it with your enviroment variables. For paths to db migrations use this one where you cloned repository with db
3) Then go to the terminal and to the folder docker where you need to run command *docker-compose up --build -d*
4) Then it is possible to check the result with comand *docker-compose ps* or to post link *http://localhost:8080/actuator/health* into a browser. The status should be UP
