# To build an image from this Dockerfile, run
# $ docker build -t photosapp-stg:latest .
#
# To export the created image into a local file, run
# $ docker save photosapp-stg:latest | gzip > photosapp-stg.tar.gz
#
# TODO User account is now created when creating the image (i.e. it is "hardcoded" into the image and not settable
#  when creating a container), change this to happen at startup (for example, a script that creates wanted user if one
#  doesn't already exist.).
# TODO Once the above is done, change ARG SERVER_UID and ARG SERVER_GID to be ENVs instead, so that they are configurable
#  for containers.

FROM gradle:jdk14

# Name of the user that will be used inside the container to run the server
ARG SERVER_UNAME=photosappuser

# UID of the user that will run the server. This user has to have
# - Read access to media directories
# - Read access to configuration file
# - Write access to meta directory
# - Write access to uploads directory
# - Write access to server directory
ARG SERVER_UID=1031

# GID of the user that will run the server
ARG SERVER_GID=100

# TCP port that the server listens to. This is the port inside the container. Port on the host (that maps to this) can
# be any port of your choosing.
ENV SERVER_PORT=3000

# Absolute path to the directory containing the server source code
ENV SERVER_SOURCES_DIR=/photosapp/server

# Absolute path to the configuration file that's used to run the server
ENV SERVER_CONFIG_FILE=/photosapp/config.json

# Adds the user specified by the variables above
RUN groupadd -g $SERVER_GID -o $SERVER_UNAME
RUN useradd -m -u $SERVER_UID -g $SERVER_GID -o -s /bin/bash $SERVER_UNAME

EXPOSE $SERVER_PORT

USER $SERVER_UNAME
WORKDIR $SERVER_SOURCES_DIR

# TODO Add loading of config file here with
#  CMD gradle run --args 'path/to/config/file.json'
CMD gradle run
