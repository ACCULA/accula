FROM node:14 as build
WORKDIR /app
COPY package.json yarn.lock /app/
RUN yarn install
COPY ./ /app/
RUN yarn build

FROM nginx:1.17
COPY --from=build /app/build/ /usr/share/nginx/html
COPY --from=build /app/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/nginx/codeclone.org.crt /etc/ssl/codeclone.org.crt
COPY --from=build /app/nginx/codeclone.org.key /etc/ssl/codeclone.org.key
