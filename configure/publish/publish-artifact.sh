#!/bin/sh
HOME=$(pwd)

VERSION=$(./mvnw -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN -Dorg.slf4j.simpleLogger.log.org.apache.maven.plugins.help=INFO | tail -1)

echo "Publish artifact, usage version is ${VERSION}"

CONF_FILE="${HOME}/configure/cloud-qiniu.conf"
if [ -f "$CONF_FILE" ]; then
  . "$CONF_FILE"
else
  echo "The configuration file $CONF_FILE does not exist, please check the path."
  exit 1
fi

PLUGIN_DIR="${HOME}/dist/plugins"
if [ ! -d "$PLUGIN_DIR" ]; then
  echo "The directory $PLUGIN_DIR does not exist, please check the path."
  exit 1
fi

echo "Bucket Name: $BUCKET_NAME"
echo "Work Home: $PLUGIN_HOME"
echo "Metaforge Home: $METAFORGE_HOME"
echo "Metadata Home: $METADATA_HOME"

printf "============================================\n"
printf "Publish artifact ...\n"

# Handle bin.tar.gz files
for file in "$HOME/dist"/*bin.tar.gz; do
  if [ -e "$file" ]; then
    filename=$(basename "$file")
    echo "Uploading binary: $filename"
    qshell fput --overwrite "$BUCKET_NAME" "$METAFORGE_HOME/versions/$VERSION/$filename" "$file"
  fi
done

# Handle metadata file
METADATA_FILE="${HOME}/configure/metadata.json"
if [ -f "$METADATA_FILE" ]; then
  # Upload new metadata file
  echo "Uploading metadata file"
  qshell fput --overwrite "$BUCKET_NAME" "$METADATA_HOME/metadata.json" "$METADATA_FILE"
fi

# Original plugin upload logic
for file in "$PLUGIN_DIR"/*.tar.gz; do
  if [ ! -e "$file" ]; then
    echo "No tar.gz files found in directory $PLUGIN_DIR."
    break
  fi

  filename=$(basename "$file")
  echo "Document found: $filename"

  case "$filename" in
    metaforge-executor-*)
      target_dir="$PLUGIN_HOME/$VERSION/executor"
      ;;
    metaforge-convert-*)
      target_dir="$PLUGIN_HOME/$VERSION/convert"
      ;;
    metaforge-fs-*)
      target_dir="$PLUGIN_HOME/$VERSION/fs"
      ;;
    metaforge-parser-*)
      target_dir="$PLUGIN_HOME/$VERSION/parser"
      ;;
    metaforge-scheduler-*)
      target_dir="$PLUGIN_HOME/$VERSION/scheduler"
      ;;
    *)
      target_dir="$PLUGIN_HOME/$VERSION/plugin"
      ;;
  esac

  echo "Uploading $filename to $target_dir"
  qshell fput --overwrite "$BUCKET_NAME" "$target_dir/$filename" "$file"
done

printf "============================================\n"