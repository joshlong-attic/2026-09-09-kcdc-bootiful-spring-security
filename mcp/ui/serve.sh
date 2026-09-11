#!/bin/bash
p=${1:-8020}
d=${2:-$PWD}
cd $d
jwebserver -d $d -p $p
