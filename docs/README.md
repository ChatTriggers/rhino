# Node.js ECMAScript compatibility tables
[node-compat-tables](https://williamkapke.github.io/node-compat-table/) is built on top of
[Kangax's hard work](https://github.com/kangax/compat-table). The majority of the credit needs to be given to the contributors
of that project.

Although [Kangax's compat table](https://github.com/kangax/compat-table) is amazing, it focuses on the entire
Javascript ecosystem. As a Node.js developer- I, thankfully, do not need to be super concerned with all of the
flavors out there. What I **do** need are deeper insights in the variations across the fast moving versions
of Node.js. So, I created [node-compat-table](https://williamkapke.github.io/node-compat-table/).

It works by [running a script](https://github.com/williamkapke/node-compat-table/blob/gh-pages/test.sh) that imports the
latest set of <s>ES6</s> ES2015, ES2016 and ES2017 tests from the [compat-table](https://github.com/kangax/compat-table) project and running
them against [several versions](https://github.com/williamkapke/node-compat-table/blob/gh-pages/v8.versions) of node PLUS
[the nightly build](https://nodejs.org/download/nightly/). The results are committed/published here.

## CLI or programmatic using

There is a Node.js module which you can programmatically check the compatibility.
Also, there is a CLI program done this work.

For details, you can check out the two repositories below:

- [node-green](https://github.com/g-plane/node-green) - Check Node.js ECMAScript compatibility programmatically.
- [node-green-cli](https://github.com/g-plane/node-green-cli) - CLI program for checking Node.js ECMAScript compatibility.

## Making change to the webpage
The webpage is hosted via GitHub.

The `build.js` file:
1) aggregates the data from the `/results` folder for the versions listed in `v8.versions`
2) uses `index.pug` to generate `index.html`

So, change `index.pug` then run:
```bash
$ node build.js
```

** Note: If a version is listed in `v8.versions` that doesn't have results generated in the `/results` directory, the
column will be empty (all white cells- no text).

## How tests are run
A scheduled task runs on Heroku runs `bash test.sh` once a day, which saves the results to the `/results` directory, rebuilds the webpage.

It then push the changes to the GitHub repo when complete.

[![js-standard-style](https://cdn.rawgit.com/feross/standard/master/badge.svg)](https://github.com/feross/standard)


## License
[MIT Copyright (c) William Kapke](https://github.com/williamkapke/node-compat-table/blob/gh-pages/LICENSE)
