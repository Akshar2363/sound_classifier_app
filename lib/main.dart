import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Audio Classifier'),
        ),
        body: const AudioClassifierWidget(),
      ),
    );
  }
}

class AudioClassifierWidget extends StatefulWidget {
  const AudioClassifierWidget({super.key});

  @override
  State<AudioClassifierWidget> createState() => _AudioClassifierWidgetState();
}

class _AudioClassifierWidgetState extends State<AudioClassifierWidget> {
  static const platform = MethodChannel('com.example.audio_classifier');
  List<Result> _results = [];
  String _error = '';
  bool _isRecording = false;

  int _numThreads = 2;
  int _numOfResults = 2;
  double _displayThreshold = 0.3;

  Future<void> initialiseClassifier() async {
    try {
      await platform.invokeMethod('initClassifier');
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to initialise audio classifier: '${e.message}'.";
      });
    }
  }

  Future<void> startAudioClassification() async {
    try {
      await platform.invokeMethod('startAudioClassification');
      setState(() {
        _isRecording = true;
      });
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to start audio classification: '${e.message}'.";
      });
    }
  }

  Future<void> stopAudioClassification() async {
    try {
      await platform.invokeMethod('stopAudioClassification');
      setState(() {
        _isRecording = false;
      });
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to stop audio classification: '${e.message}'.";
      });
    }
  }

  Future<void> updateNumThreads() async {
    try {
      await platform.invokeMethod('updateNumThreads', {'numThreads': _numThreads});
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to update number of threads: '${e.message}'.";
      });
    }
  }

  Future<void> updateNumOfResults() async {
    try {
      await platform.invokeMethod('updateNumOfResults', {'numOfResults': _numOfResults});
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to update number of results: '${e.message}'.";
      });
    }
  }

  Future<void> updateDisplayThreshold() async {
    try {
      await platform.invokeMethod('updateDisplayThreshold', {'displayThreshold': _displayThreshold.toString()});
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to update display threshold: '${e.message}'.";
      });
    }
  }

  void clearResults() {
    setState(() {
      _results = [];
    });
  }

  @override
  void initState() {
    super.initState();
    initialiseClassifier();
    platform.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onResult':
          List<dynamic> results = call.arguments;
          setState(() {
            _results = results.map((result) {
              return Result(
                label: result['label'],
                score: result['score'],
              );
            }).toList();
          });
          break;
        case 'onError':
          setState(() {
            _error = call.arguments;
          });
          break;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              itemCount: _results.length,
              itemBuilder: (context, index) {
                final result = _results[index];
                return ListTile(
                  title: Text(result.label),
                  subtitle: LinearProgressIndicator(
                    value: result.score,
                  ),
                );
              },
            ),
          ),
          if (_error.isNotEmpty)
            Text(
              _error,
              style: TextStyle(color: Colors.red),
            ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Number of Threads: $_numThreads'),
              IconButton(
                icon: const Icon(Icons.remove),
                onPressed: () {
                  setState(() {
                    if (_numThreads > 1) _numThreads--;
                    updateNumThreads();
                  });
                },
              ),
              IconButton(
                icon: const Icon(Icons.add),
                onPressed: () {
                  setState(() {
                    if (_numThreads < 4) _numThreads++;
                    updateNumThreads();
                  });
                },
              ),
            ],
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Number of Results: $_numOfResults'),
              IconButton(
                icon: const Icon(Icons.remove),
                onPressed: () {
                  setState(() {
                    if (_numOfResults > 1) _numOfResults--;
                    updateNumOfResults();
                  });
                },
              ),
              IconButton(
                icon: const Icon(Icons.add),
                onPressed: () {
                  setState(() {
                    if (_numOfResults < 5) _numOfResults++;
                    updateNumOfResults();
                  });
                },
              ),
            ],
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Display Threshold: ${_displayThreshold.toStringAsFixed(1)}'),
              IconButton(
                icon: const Icon(Icons.remove),
                onPressed: () {
                  setState(() {
                    if (_displayThreshold > 0.2) {
                      _displayThreshold -= 0.1;
                    }
                    updateDisplayThreshold();
                  });
                },
              ),
              IconButton(
                icon: const Icon(Icons.add),
                onPressed: () {
                  setState(() {
                    if (_displayThreshold < 0.8) {
                      _displayThreshold += 0.1;
                    }
                    updateDisplayThreshold();
                  });
                },
              ),
            ],
          ),
        ],
      )
    );
  }
}

class Result {
  final String label;
  final double score;

  Result({required this.label, required this.score});
}
