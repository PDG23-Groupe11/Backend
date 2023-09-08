# Déployement avec Kubernetes
Nous utilisons Kubernetes pour déployer automatiquement ce backend

Ce backend tourne sur Google Kubernetes Engine, mais ces fichiers ne sont pas exclusif à ce service, et pourraient très bien être utilisés pour être déployés ailleurs,
raison pourquoi cette documentation ne va pas en détails sur la configuration nécessaire du côté Helm Kubernetes.

## Secret
Pour pouvoir passer les identifiants de connexion à la base de donnée, [api-svc.yaml](api-svc.yaml) est utilisé.\
Il faut y encoder les secrets en base64. Exemple sous linux :
```shell
echo "mon secret" | base64
```
`POSTGRES_URL` doit contenir une url comprenant le driver jdbc, exemple : `jdbc:postgresql://theDbHost.com:5432`

Une fois fait, le secret peut être appliqué
```shell
kubectl create -f api-secrets.yaml
```

## Déployement et Service
Une fois les secrets en ligne, le déployment du container du backend et le service associés peuvent être créés

Dans notre cas le déployement va chercher l'image sur DockerHub, puisque notre CI/CD l'envoie là bas à chaque release. 
Il faut donc au besoin changer le nom de l'image pour y faire pointer sur une image que l'on aurait build en local.
```shell
kubectl create -f api-deploy.yaml
kubectl create -f api-svc.yaml
```
Une fois fait, et étant donné que la base de donnée pointée par les secrets tourne bien, le service devrait être en ligne! 
