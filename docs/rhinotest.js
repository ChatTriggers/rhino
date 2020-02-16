var testers = JSON.parse(readFile('./testers.json'));

// We'll need this for the tests
var global = {};

// This function is needed to run the tests and was extracted from:
// https://github.com/kangax/compat-table/blob/gh-pages/node.js
global.__createIterableObject = function (arr, methods) {
  methods = methods || {};
  if (typeof Symbol !== 'function' || !Symbol.iterator) {
    return {};
  }

  arr.length++;
  var iterator = {
    next: function () {
      return {
        value: arr.shift(),
        done: arr.length <= 0
      }
    },
    'return': methods['return'],
    'throw': methods['throw']
  };
  var iterable = {};
  iterable[Symbol.iterator] = function () {
    return iterator
  };

  return iterable
};

var output = {
  _version: 'UNKNOWN',
  _engine: 'Rhino',
};

let activeAsyncTests = 0;
var shouldWrite = false;

var interval = setInterval(function () {
  if (shouldWrite && activeAsyncTests === 0) {
    write();
    clearInterval(interval);
  }
}, 100);

var versions = Object.keys(testers);

function next(ver) {
  if (!ver) {
    shouldWrite = true;
    return;
  }

  var completed = 0;
  var results = output[ver] = {
    _successful: 0,
    _count: Object.keys(testers[ver]).length,
    _percent: 0
  };
  Object.keys(testers[ver]).forEach(function (name) {
    var script = testers[ver][name];
    results[name] = false; // make SURE it makes it to the output

    run(name, script, function (result) {
      if (/asyncTestPassed/.test(script)) {
      }
      // expected results: `e.message` or true/false;
      results[name] = typeof result === 'string' ? result : !!result;
      if (results[name] === true) results._successful++;

      if (++completed === results._count) {
        results._percent = results._successful / results._count;
        // In the future this needs to become setTimeout
        // so that we can support Promises
        next(versions.pop());
      }
    })
  })
}

next(versions.pop());

function run(name, script, cb) {
  // Work around a regexp bug in older Rhinos
  if (/incomplete patterns and quantifiers/.test(name)) {
    return cb(false);
  }

  // kangax's Promise tests reply on a asyncTestPassed function.
  var async = /asyncTestPassed/.test(script);
  if (async) {
    runAsync(script, cb);
  } else {
    cb(runSync(script));
  }
}

function runAsync(script, cb) {
  activeAsyncTests += 1;
  let caught = false;
  let passed = false;
  try {
    script += ';setTimeout(() => asyncTestFailed(), 5000);';
    // print('======== async ========');
    // print(script);
    var fn = new Function('asyncTestPassed', 'asyncTestFailed', script);

    fn(function () {
      passed = true;
      activeAsyncTests -= 1;
      cb(true);
    }, function () {
      if (!caught && !passed) {
        activeAsyncTests -= 1;
        cb(false);
      }
    });
  } catch (e) {
    caught = true;
    activeAsyncTests -= 1;
    cb(e.message)
  }
}

function runSync(script) {
  try {
    // print('==================');
    // print(script);
    var fn = new Function(script);
    return fn() || false
  } catch (e) {
    return e !== undefined && e !== null ? e.message : "Empty error"
  }
}

function write() {
  print(JSON.stringify(output, null, 2));
}
