import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/mekanismae"
MODELS = ASSETS / "models/block"
BLOCKSTATES = ASSETS / "blockstates"

MACHINES = (
    "me_crusher",
    "me_energized_smelter",
    "me_metallurgic_infuser",
    "me_osmium_compressor",
    "me_purification_chamber",
    "me_chemical_injection_chamber",
    "me_combiner",
    "me_precision_sawmill",
)


def model_data(machine, online, working):
    state = "online" if online else "offline"
    working_suffix = "_working" if working else ""
    return {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": f"mekanismae:block/me_machine_chassis_right_{state}",
            "down": "mekanismae:block/me_machine_chassis_bottom",
            "up": f"mekanismae:block/me_machine_chassis_top_{state}",
            "north": f"mekanismae:block/{machine}_front_{state}{working_suffix}",
            "south": f"mekanismae:block/me_machine_chassis_back_{state}",
            "west": f"mekanismae:block/me_machine_chassis_left_{state}",
            "east": f"mekanismae:block/me_machine_chassis_right_{state}",
        },
    }


def model_name(machine, online, working):
    if not online and not working:
        return machine
    return f"{machine}_{'online' if online else 'offline'}{'_working' if working else ''}"


def blockstate_data(machine):
    variants = {}
    rotations = {"north": None, "east": 90, "south": 180, "west": 270}
    for online in (False, True):
        for working in (False, True):
            name = model_name(machine, online, working)
            for facing, rotation in rotations.items():
                value = {"model": f"mekanismae:block/{name}"}
                if rotation is not None:
                    value["y"] = rotation
                variants[f"facing={facing},online={str(online).lower()},working={str(working).lower()}"] = value
    return {"variants": variants}


def save_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    MODELS.mkdir(parents=True, exist_ok=True)
    BLOCKSTATES.mkdir(parents=True, exist_ok=True)
    for machine in MACHINES:
        for online in (False, True):
            for working in (False, True):
                save_json(MODELS / f"{model_name(machine, online, working)}.json",
                          model_data(machine, online, working))
        save_json(BLOCKSTATES / f"{machine}.json", blockstate_data(machine))


if __name__ == "__main__":
    main()
