Dockerification achieved thanks to this guide:
https://auth0.com/blog/use-docker-to-create-a-node-development-environment/

To start the container, run
```
docker-compose run --rm --service-ports nodeserver
```
`--rm` removes the container when it stops

Then to start the server, run
```
npm start
```