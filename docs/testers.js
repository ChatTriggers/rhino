// this files patches the 2 `tests-xx.json` files together in 1 object organized by ES version

var testers = {
  ES2015: require('./testers-es6.json'),
  ES2016: {},
  ES2017: {},
  ES2018: {},
  ESNEXT:  require('./testers-esnext.json')
}

var esnext = require('./testers-es2016plus.json')
Object.keys(esnext).forEach((key) => {
  if (/^2016/.test(key)) {
    testers.ES2016[key.substr(5)] = esnext[key]
  }

  if (/^2017/.test(key)) {
    testers.ES2017[key.substr(5)] = esnext[key]
  }

  if (/^2018/.test(key)) {
    testers.ES2018[key.substr(5)] = esnext[key]
  }
})
console.log(JSON.stringify(testers, null, 2))
