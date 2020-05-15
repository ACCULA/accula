FROM node:14 as build
WORKDIR /app
COPY package.json /app/
RUN yarn install
COPY ./ /app/
RUN yarn run build

FROM nginx:1.17
COPY --from=build /app/build/ /usr/share/nginx/html
COPY --from=build /app/nginx.conf /etc/nginx/conf.d/default.conf
