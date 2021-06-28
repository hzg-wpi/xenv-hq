CID=`docker ps -aqf "name=tango-cs"`
echo $CID
docker exec $CID /usr/local/bin/tango_admin --add-server HeadQuarter/dev HeadQuarter dev/xenv/hq
docker exec $CID /usr/local/bin/tango_admin --add-server HeadQuarter/dev XenvManager dev/xenv/manager
docker exec $CID /usr/local/bin/tango_admin --add-server HeadQuarter/dev ConfigurationManager dev/xenv/config