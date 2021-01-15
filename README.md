# How to run

```shell

export REGION=<your region her>

export PROJECT=<your project here>

export BUCKET=<your bucket here>

./gradlew goJF clean shadowJar

java -jar build/libs/slowly-1.0.0.jar --runner=DataflowRunner \
  --project=$PROJECT \
  --region=$REGION \
  --network=default \
  --usePublicIps=false \
  --tempLocation=gs://$BUCKET/staging \
  --refreshDuration=60

```