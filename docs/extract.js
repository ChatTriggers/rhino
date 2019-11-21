var es6 = require(process.argv[2])
var tests = es6.tests
var testers = {}
var category

function deindent (fn) {
  var indent = /(?:^|\n)([\t ]+)[^\n]+/.exec(fn)
  return indent ? fn.replace(new RegExp('\n' + indent[1], 'g'), '\n') : fn
}

tests.forEach(function (test) {
  if (category !== test.category) {
    category = test.category
  }

  var name = [category, test.name]

  if (test.subtests) {
    test.subtests.forEach(function (subtest) {
      name[2] = subtest.name
      testers[name.join('›').replace(/<[^>]+>/g, '')] = getScript(subtest.exec)
    })
  } else {
    testers[name.join('›').replace(/<[^>]+>/g, '')] = getScript(test.exec)
  }

  function getScript (fn) {
    return deindent(fn + '').split('\n').slice(1, -1).join('\n')
  }
})

console.log(
  JSON.stringify(testers, null, 2)
)
