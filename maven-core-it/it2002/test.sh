#!/bin/bash


echo
echo "*********************************************************"
echo if you are running in windows, run $0 windows
echo "*********************************************************"
echo


rm -Rf target

mkdir target

svnadmin create --fs-type fsfs target/svnroot

dir=`readlink -f ${PWD}`

if [ "$1" == "windows" ]; then
  dir=`cygpath -m $dir`
  echo setting dir to $dir
fi

svn import project file://localhost/${dir}/target/svnroot/trunk/project -m "import."
svn mkdir file://localhost/${dir}/target/svnroot/tags -m "Creating tags dir."

svn co file://localhost/${dir}/target/svnroot/trunk/project target/project.checkout

cd target/project.checkout

cat pom.xml | sed "s#\${project.file.parentFile.parentFile}#$dir#g" >tmp
mv tmp pom.xml

svn ci -m 'update scm' pom.xml

rm -Rf target

mvn clean install
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

mvn -DgenerateReleasePoms=true -e release:prepare -Denv=test -B -Dtag=test-tag
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

mvn -DreleasePom=release-pom.xml -e release:perform -Denv=test
ret=$?; if [ $ret != 0 ]; then exit $ret; fi

