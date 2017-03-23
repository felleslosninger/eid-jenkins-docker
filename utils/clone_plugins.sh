CLONE_URL=$1
if [ "${CLONE_URL}" = "" ]
then
echo "Must specify jenkins baseurl"
exit 0
fi
PLUGINS=$(curl -s "${CLONE_URL}/pluginManager/api/xml?depth=1&xpath=/*/*/shortName|/*/*/version&wrapper=plugins")
PLUGINS_RUN=$(echo $PLUGINS|perl -pe 's/.*?<shortName>([\w-]+).*?<version>([^<]+)()(<\/\w+>)+/RUN install-plugin.sh \1 \2;/g')
grep -v "RUN install-plugin.sh" Dockerfile>tmp_Dockerfile
sed -i  "s/^EXPOSE 8080/\\n${PLUGINS_RUN}\nEXPOSE 8080/g" tmp_Dockerfile
sed -i  "s/\;RUN/\\nRUN/g" tmp_Dockerfile
sed -i  "s/\;$/\\n/g" tmp_Dockerfile
mv tmp_Dockerfile Dockerfile

