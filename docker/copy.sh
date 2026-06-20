#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql
echo "begin copy sql "
cp ../sql/ry_20260402.sql ./mysql/db
cp ../sql/ry_config_20260311.sql ./mysql/db

# copy html
echo "begin copy html "
cp -r ../mall-ui/dist/** ./nginx/html/dist


# copy jar
echo "begin copy mall-gateway "
cp ../mall-gateway/target/mall-gateway.jar ./mall/gateway/jar

echo "begin copy mall-auth "
cp ../mall-auth/target/mall-auth.jar ./mall/auth/jar

echo "begin copy mall-visual "
cp ../mall-visual/mall-monitor/target/mall-visual-monitor.jar  ./mall/visual/monitor/jar

echo "begin copy mall-modules-system "
cp ../mall-modules/mall-system/target/mall-modules-system.jar ./mall/modules/system/jar

echo "begin copy mall-modules-file "
cp ../mall-modules/mall-file/target/mall-modules-file.jar ./mall/modules/file/jar

echo "begin copy mall-modules-job "
cp ../mall-modules/mall-job/target/mall-modules-job.jar ./mall/modules/job/jar

echo "begin copy mall-modules-gen "
cp ../mall-modules/mall-gen/target/mall-modules-gen.jar ./mall/modules/gen/jar

