# Grocerypal - backend

API codée en Kotlin avec Ktor, intéragissant avec une base de donnée Postgres

## Structure de base
Le modèle de donnée se trouve dans [src/main/kotlin/ch/heigvd/database](src/main/kotlin/ch/heigvd/database).\
Dedans, le fichier Database.kt définit également toutes les routes de l'API permettant d'interagir avec la base de donnée

Le Routing pour la petite page de status, et le système de gestion d'images de recette se trouve dans
[src/main/kotlin/ch/heigvd/plugins/Routing.kt](src/main/kotlin/ch/heigvd/plugins/Routing.kt) 

## Configuration
Fait par variable d'environnement, passée dans [src/main/resources/application.conf](src/main/resources/application.conf)
et [src/main/resources/application-test.conf](src/main/resources/application-test.conf) (ce deuxième servant aux tests automatisés)

| Variable            | Description                                                                                           |
|---------------------|-------------------------------------------------------------------------------------------------------|
| `POSTGRES_URL`      | l'URL vers le serveur postgres, comprenant le driver jdb. ex : `jdbc:postgresql://theDbHost.com:5432` |
| `POSTGRES_USER`     | Utilisateur avec des droits sur la DB `Grocerypal`                                                    |
| `POSTGRES_PASSWORD` | Mot de passe associé                                                                                  |


## Lancement
Une fois les variables environment configurée selon l'étape précédente, il est possible de lancer le projet.

Sur IntelliJ, il faut configurer un projet Ktor. Il peut ensuite être lancé et debuggé comme tout autre projet.

### Build
`./gradlew build`

### Test
Les variables d'environnement doivent pointer sur une base de donnée comprenant les données de test 
(voir [documentation dans le repos Documentation](https://github.com/PDG23-Groupe11/Documentation/blob/main/README.md))

`./gradlew test`

### Build d'exécutable final
`./gradlew installDist`
Trouvable ensuite dans [build/install/Grocerypal-backend](build/install/Grocerypal-backend)

C'est le livrable qui sera mis dans l'image Docker [par le CI/CD](https://github.com/PDG23-Groupe11/Documentation/blob/main/guides_utilisation_installation.md)

### Kubernetes
Pour le déployement avec Kubernetes, voir [./kubernetes/readme.md](kubernetes/readme.md)

## Documentation Swagger
Une documentation de l'API tourne avec swagger, accessible au chemin `/openapi` sur l'application

Elle permet non seulement de documenter tous les endpoints offerts, mais aussi de les essayer

Elle est définie ici : [src/main/resources/openapi/documentation.yaml](src/main/resources/openapi/documentation.yaml)

## Image de recette
Nous n'avons pas eu le temps de configurer de la persistence pour les images des recettes, il faut donc malheureusement
réuploader toutes les images après chaque release.

Les images peuvent être POSTées sur l'endpoint `/static/recipeImages/{recipeId}` (voir Swagger)
