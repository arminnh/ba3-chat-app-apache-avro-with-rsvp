for i in $(git status | grep "modified:"); do if [[ $i != "modified:" ]]; then echo $i >> .gitignore; fi; done

