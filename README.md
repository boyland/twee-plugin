# twee-plugin
Eclipse plugin for Twee3 + SugarCube
## Version
Twee Plugin version 0.5
* outline view available

Twee Plugin version 0.4
* macro checking enabled
* new file wizard, including creating an IFID
* highlighting copied from default JavaScript syntax highlighting

Twee Plugin version 0.2
* plugin enabled with drop-in JAR
* spell-checking of regular text
* editor highlight support for files with `.tw` extension
## License
Originally written by John Boyland <boyland@uwm.edu>.
This project is released to the public domain.
It may be used/copied/modified in any way.
The author retains no rights.
## Purpose
Provide editor support for Twee3 code using SugarCube.  Currently it only provides highlighting.
## Installation
The release page has a JAR file that can be dropped in the `plugins` folder of your Eclipse installation.  
Or you can clone the whole project into an Eclipse workspace with Java and Plugin Development support, and then run an Eclipse workbench within it using this plugin.
## Desired New Features
* Highlight [script] pages as JavaScript, not Twee
  (prevent spell checker from checking.)
* "type" checking of SC macro parameters
* check nesting of SC macros
* check HTML
* check JavaScript (TweeScript) for syntactic correctness
## Documentation
### Macro definition syntax
If you use non-standard SugarCube macros (or declare your own widgets), you will need to declare them in a JSON file or uses of them will be marked as incorrect.  The JSON file has one value for each macro.  Most generally this is a JSON object with the following properties:
* argumentSyntax: one of the strings `NORMAL`, `EXPRESSION`, or `COMMASEP`;
  The first is for macros which space separate arguments, the second takes a *TwineScript* expression. The latter is for comma-separated lists.
* minArguments, maxArguments: integers;
  The minimum (default 0) and maximum (default, same as minimum) number of arguments this macro can take.  Most relevant for `NORMAL` macros.
* isNested: boolean;
  If true, this macro call can only appear inside of another macro's body.
* nestedMacros: array;
  If non-null, the names of macros that may only appear directly nested in this one, or if `isNested` then the names of amcros that use this macro inside them.
For example, here is a small excerpt of pre-defined macros:
``` json
{
	"if": {
		"argumentSyntax": "EXPRESSION",
		"nestedMacros": [
			"elseif",
			"else"
		]
	},
	"elseif": {
		"argumentSyntax": "EXPRESSION",
		"nestedMacros": [
			"if"
		],
		"isNested": true
	},
	"else": {
		"nestedMacros": [
			"if"
		],
		"isNested": true
	}
}
```
Even thought it's redundant, entries for the nested macros (e.g. "else") are needed.