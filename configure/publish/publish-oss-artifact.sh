#!/bin/sh
HOME=$(pwd)

VERSION=$(./mvnw -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN -Dorg.slf4j.simpleLogger.log.org.apache.maven.plugins.help=INFO | tail -1)

echo "Publish artifact, usage version is ${VERSION}"

CONF_FILE="${HOME}/configure/cloud.conf"
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

LOGO_DIR="${HOME}/logo"
if [ ! -d "$LOGO_DIR" ]; then
  echo "The directory $LOGO_DIR does not exist, please check the path."
  exit 1
fi

echo "OSS Bucket Name: $BUCKET_NAME"
echo "OSS Endpoint: $OSS_ENDPOINT"
echo "Work Home: $PLUGIN_HOME"
echo "Metaforge Home: $METAFORGE_HOME"
echo "Metadata Home: $METADATA_HOME"
echo "Resource Home: $RESOURCE_HOME"

if ! command -v ossutil > /dev/null; then
  echo "Error: ossutil is not installed. Please install it first."
  echo "You can download it from: https://help.aliyun.com/document_detail/120075.html"
  exit 1
fi

printf "============================================\n"
printf "Publish artifact ...\n"

# Handle bin.tar.gz files
for file in "$HOME/dist"/*bin.tar.gz; do
  if [ -e "$file" ]; then
    filename=$(basename "$file")
    echo "Uploading binary: $filename"
    ossutil cp "$file" "oss://$BUCKET_NAME/$METAFORGE_HOME/versions/$VERSION/$filename" -f
  fi
done

# Handle metadata file
METADATA_FILE="${HOME}/configure/metadata.json"
if [ -f "$METADATA_FILE" ]; then
  # Upload new metadata file
  echo "Uploading metadata file"
  ossutil cp "$METADATA_FILE" "oss://$BUCKET_NAME/$METADATA_HOME/metadata.json" -f
fi

# Handle logo files
echo "Processing logo files..."
for type in convert executor fs plugin scheduler; do
  if [ -d "$LOGO_DIR/$type" ]; then
    for logo in "$LOGO_DIR/$type"/*.svg; do
      if [ -f "$logo" ]; then
        filename=$(basename "$logo")
        echo "Uploading logo: $type/$filename"
        ossutil cp "$logo" "oss://$BUCKET_NAME/$RESOURCE_HOME/logo/$type/$filename" -f
      fi
    done
  fi
done

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
  ossutil cp "$file" "oss://$BUCKET_NAME/$target_dir/$filename" -f
done

printf "============================================\n"