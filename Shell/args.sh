#!/bin/bash
echo "args length: $#"
for arg in "$@";do
	echo -e $arg
done
echo ""
for arg in "$*";do
	echo $arg
done
