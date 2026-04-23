# Review en verbetering: routine builder flow

## Samenvatting
De routine builder liet de interne datastructuur te zichtbaar worden voor de eindgebruiker. Technisch bestaat een routine uit `WorkoutRoutine -> WorkoutDay -> WorkoutExercisePlan`, maar in de UI voelde dit alsof je eerst verplicht een dagnaam moest bedenken voordat je gewoon oefeningen kon toevoegen. Dat is onnodige cognitieve belasting tijdens het bouwen van een schema.

De v1-verbetering houdt het bestaande datamodel intact, zonder database-migratie. De app behandelt een `WorkoutDay` voortaan meer als een optionele sessie-groep: als een gebruiker bij een lege routine direct een oefening toevoegt, maakt TrainIQ automatisch `Session 1` aan.

## Gevonden frictie
- De `Train` tab combineert routine-aanmaak, actieve routine, routine editing, exercise library en history in een lange scroll. Daardoor voelt routine bouwen minder gefocust dan workout starten.
- Oefeningen konden pas worden toegevoegd nadat de gebruiker handmatig een workout day had aangemaakt.
- De labels `Add workout day` en `Day name` maakten de technische laag leidend, terwijl gebruikers meestal denken in sessies zoals `Push`, `Upper`, `Full Body` of gewoon `Workout A`.
- Sets, rep range en rust stonden op dag-niveau vlak boven `Add exercise`, waardoor het niet direct duidelijk was dat deze waarden op de volgende oefening worden toegepast.
- Oefeningen werden compact weergegeven als `3 sets - 8-12`, zonder rusttijd of set type in dezelfde scanbare regel.

## Geïmplementeerde v1
- Een nieuwe routine-level add-flow is toegevoegd: `addExerciseToRoutine`.
- Als een routine nog geen sessies heeft, maakt de repository automatisch een eerste sessie aan met de naam `Session 1`.
- Lege sessienamen zijn toegestaan bij het handmatig toevoegen van een sessie; de repository vult dan `Session N` in.
- In de routinekaart is de lege staat veranderd naar een directe CTA: `Add first exercise`.
- De UI-copy gebruikt nu `session` in plaats van verplichte `day`-taal.
- De oefeningweergave gebruikt scanbare chips voor `sets`, `reps`, `rest` en `set type`, bijvoorbeeld `3 sets`, `8-12 reps`, `90s rest`, `Working`.
- Unit tests dekken dat een lege routine automatisch een default sessie krijgt en dat bestaande sessies en oefenvolgorde behouden blijven.

## Aanbevolen volgende UX-stap
Splits de `Train` tab in duidelijke subsecties of tabs:

- `Today`: actieve routine, volgende sessie, start workout.
- `Routines`: routine overzicht en focused builder.
- `Library`: oefenbibliotheek en custom exercises.
- `History`: afgeronde workouts en sessiegeschiedenis.

Daarna kan routine bouwen naar een dedicated builder screen of modal flow. In die flow is `Add exercise` de primaire actie, en sessienamen zijn secundair: zichtbaar, aanpasbaar, maar geen poort om te starten.

## Vervolg voor set/rep editing
De huidige v1 verbetert de presentatie, maar per-exercise editing van sets, reps, rust en set type verdient een aparte wijziging. Daarvoor is een repository/use-case nodig om bestaande `WorkoutExercisePlan` records bij te werken. Aanbevolen gedrag:

- Tik op een oefening of edit-icoon opent een compacte editor.
- Velden: sets, rep range, rest seconds, set type, superset.
- De dag/sessie defaults blijven bestaan als snelle invoer voor nieuwe oefeningen, maar overschrijven bestaande oefeningen niet automatisch.

## Acceptatiecriteria
- Een gebruiker kan een blank routine maken en direct een oefening toevoegen zonder eerst een dagnaam te typen.
- TrainIQ maakt automatisch `Session 1` aan als technische container.
- Bestaande routines met sessies blijven hun sessienaam en oefenvolgorde behouden.
- Oefeningen in de builder tonen sets, reps, rust en set type scanbaar.
- De implementatie gebruikt het bestaande Clean/MVVM/UDF patroon en vereist geen database-migratie.
